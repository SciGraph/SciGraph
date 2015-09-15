package io.scigraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class LoadIndex {
  public static void main(String[] args) throws ElasticsearchException, IOException {
    final String path = "/tmp/index-dump/";
    final String subclassPath = path + "subclass/";
    final String superclassPath = path + "superclass/";

    // on startup
    System.out.println("Starting indexing...");

    Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

    File directory = new File(superclassPath);
    File[] fList = directory.listFiles();
    for (File file : fList) {
      if (file.isFile()) {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String str = new String(data, "UTF-8");
        IndexResponse response = client.prepareIndex("scigraph", "superclass").setSource(str).execute().actionGet();
      }
    }

    directory = new File(subclassPath);
    fList = directory.listFiles();
    for (File file : fList) {
      if (file.isFile()) {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String str = new String(data, "UTF-8");
        IndexResponse response = client.prepareIndex("scigraph", "subclass").setSource(str).execute().actionGet();
      }
    }


    // on shutdown
    System.out.println("Finished indexing and shutting down...");
    client.close();
  }
}
