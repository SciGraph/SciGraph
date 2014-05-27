/**
 * Copyright (C) 2014 Christopher Condit (condit@sdsc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sdsc.scigraph.owlapi;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

import com.google.inject.Inject;

/**
 * Cache remote IRIs as a local file. Allow periodic reloads of the original IRI
 * for updates.
 */
@Singleton
class FileCachingIRIMapper implements OWLOntologyIRIMapper {

  private static final Logger logger = Logger.getLogger(FileCachingIRIMapper.class.getName());

  private final FileValidity validityHelper;
  private final File cacheDirectory;
  private FileCachingFilter filter = null; 

  public static interface FileCachingFilter {

    public boolean allowCaching(IRI iri);
  }

  @Inject
  FileCachingIRIMapper() throws IOException {
    super();
    Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx");
    Path tempPath = Files
        .createTempDirectory("owlapi", PosixFilePermissions.asFileAttribute(perms));
    cacheDirectory = tempPath.toFile();

    validityHelper = new FileValidity(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES));
  }

  @Inject(optional=true)
  void setFilter(FileCachingFilter filter) {
    this.filter = filter;
  }

  protected void reloadIRIs() {
    validityHelper.setInvalidRecursive(cacheDirectory);
  }

  protected InputStream getInputStream(IRI iri) throws IOException {
    CloseableHttpClient client = HttpClientBuilder.create()
    .setRedirectStrategy(new DefaultRedirectStrategy())
    .setRetryHandler(new DefaultHttpRequestRetryHandler())
    .build();
    final HttpGet request = new HttpGet(iri.toURI());
    return tryHttpRequest(client, request, iri, 3);
  }

  private InputStream tryHttpRequest(CloseableHttpClient client, HttpGet request, IRI iri, int count) throws IOException {
    // try the load
    final HttpResponse response;
    try {
      response = client.execute(request);
    }
    catch (IOException e) {
      if (count <= 0) {
        // no more retry, handle the final error.
        return handleError(iri, e);
      }
      logger.warning("Retry request for IRI: "+iri+" after exception: "+e.getMessage());
      defaultRandomWait();
      return tryHttpRequest(client, request, iri, count - 1);
    }
    final HttpEntity entity = response.getEntity();
    final StatusLine statusLine = response.getStatusLine();
    if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
      StringBuilder message = new StringBuilder();
      message.append("Web request for IRI '");
      message.append(iri);
      message.append("' failed with status code: ");
      message.append(statusLine.getStatusCode());
      String reasonPhrase = statusLine.getReasonPhrase();
      if (reasonPhrase != null) {
        message.append(" reason: ");
        message.append(reasonPhrase);
      }
      EntityUtils.consume(entity);
      if (count <= 0) {
        // no more retry, handle the final error.
        return handleError(iri, new IOException(message.toString()));
      }
      message.append("\n Retry request.");
      logger.warning(message.toString());

      defaultRandomWait();

      // try again
      return tryHttpRequest(client, request, iri, count - 1);
    }
    return entity.getContent();
  }

  private void defaultRandomWait() {
    // wait a random interval between 400 and 1500 ms
    randomWait(400, 1500);
  }

  private void randomWait(int min, int max) {
    Random random = new Random(System.currentTimeMillis());
    long wait = min + random.nextInt((max - min));
    try {
      logger.fine("Waiting "+wait+" ms for retry.");
      Thread.sleep(wait);
    } catch (InterruptedException exception) {
      logger.log(Level.WARNING, "Interrupted sleep: Incomplete wait for retry.", exception);
    }
  }

  /**
   * Overwrite this method to implement more sophisticated methods. E.g.,
   * fall-back on local copies, if the URL is not reachable.
   * 
   * @param iri
   * @param exception
   * @return inputStream
   * @throws IOException
   */
  protected InputStream handleError(IRI iri, IOException exception) throws IOException {
    logger.log(Level.WARNING, "IOException during fetch of IRI: "+iri, exception);
    throw exception;
  }

  @Override
  public IRI getDocumentIRI(IRI ontologyIRI) {
    if (filter != null && filter.allowCaching(ontologyIRI) == false) {
      return null;
    }
    return mapIRI(ontologyIRI);
  }

  private synchronized IRI mapIRI(IRI originalIRI) {
    boolean success = false;
    File localFile = localCacheFile(originalIRI);
    if (isValid(localFile)) {
      success = true;
    }
    else{
      createFile(localFile);
      success = download(originalIRI, localFile);
    }
    if (success) {
      return IRI.create(localFile);
    }
    return null;
  }

  protected boolean createFile(File localFile) {
    File folder = localFile.getParentFile();
    try {
      FileUtils.forceMkdir(folder);
      localFile.createNewFile();
      return true;
    } catch (IOException exception) {
      logger.log(Level.WARNING, "Could not create local file: "+localFile.getAbsolutePath(), exception);
      return false;
    }
  }

  private boolean download(IRI originalIRI, File localFile) {
    try (InputStream inputStream = getInputStream(originalIRI);
        OutputStream outputStream = new FileOutputStream(localFile)){
      logger.info("Downloading: "+originalIRI+" to file: "+localFile.getAbsolutePath());
      if (inputStream == null) {
        return false;
      }
      IOUtils.copy(inputStream, outputStream);
      outputStream.close();
      setValid(localFile);
      return true;
    } catch (IOException exception) {
      logger.log(Level.WARNING, "Could not download IRI: "+originalIRI, exception);
      return false;
    }
  }

  protected void setValid(File localFile) throws IOException {
    validityHelper.setValid(localFile);
  }

  protected boolean isValid(File localFile) {
    return localFile.exists() && validityHelper.isValid(localFile);
  }

  private File localCacheFile(IRI iri) {
    return new File(cacheDirectory, localCacheFilename(iri));
  }

  static String localCacheFilename(IRI iri) {
    URI uri = iri.toURI();
    StringBuilder sb = new StringBuilder();
    escapeToBuffer(sb, uri.getHost());
    escapeToBuffer(sb, uri.getPath());
    return sb.toString();
  }

  static void escapeToBuffer(StringBuilder sb, String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (Character.isLetterOrDigit(c) || c == '.' || c == '-') {
        sb.append(c);
      }
      else if (c == '/') {
        sb.append(File.separatorChar);
      }
      else {
        sb.append('_');
      }
    }
  }

  /**
   * Helper for checking, whether a file is still valid
   */
  static class FileValidity {

    private static final String VALIDITY_FILE_SUFFIX = ".validity";

    private static final class ValidityFileFilter implements FileFilter {

      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().endsWith(VALIDITY_FILE_SUFFIX);
      }
    }

    private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {

      @Override
      protected DateFormat initialValue() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      }
    };

    private final long validPeriod;

    /**
     * @param validPeriod
     */
    FileValidity(long validPeriod) {
      super();
      this.validPeriod = validPeriod;
    }

    private File getValidityFile(File file) {
      return new File(file.getParentFile(), file.getName() + VALIDITY_FILE_SUFFIX);
    }

    private Date getDate(File validityFile) {
      if (validityFile.exists()) {
        try {
          String dateString = FileUtils.readFileToString(validityFile);
          return df.get().parse(dateString);
        } catch (IOException exception) {
          FileUtils.deleteQuietly(validityFile);
        } catch (ParseException exception) {
          validityFile.delete();
        }
      }
      return null;
    }

    private void setDate(Date date, File validityFile) throws IOException {
      FileUtils.write(validityFile, df.get().format(date));
    }

    private void deleteValidityFile(File file) {
      File validityFile = getValidityFile(file);
      if (validityFile.exists()) {
        validityFile.delete();
      }
    }

    void setValid(File file) throws IOException {
      setDate(new Date(), getValidityFile(file));
    }

    void setInvalid(File file) {
      deleteValidityFile(file);
    }

    void setInvalidRecursive(File directory) {
      File[] files = directory.listFiles(new ValidityFileFilter());
      if (files != null) {
        for (File file : files) {
          if (file.isDirectory()) {
            setInvalidRecursive(file);
          }
          else {
            file.delete();
          }
        }
      }
    }

    boolean isValid(File file) {
      Date date = getDate(getValidityFile(file));
      if (date != null) {
        Date earliest = new Date(System.currentTimeMillis() - validPeriod);
        return date.after(earliest);
      }
      return false;
    }
  }

}