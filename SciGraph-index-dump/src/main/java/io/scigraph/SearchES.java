package io.scigraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterators;

public class SearchES {
  public static void main(String[] args) throws IOException, InterruptedException {
    GraphDatabaseService graphDb2 = new GraphDatabaseFactory().newEmbeddedDatabase("/tmp/dipper-test");
    Transaction tx2 = graphDb2.beginTx();

    Stopwatch stopwatch = Stopwatch.createStarted();
    String url = "http://localhost:9200/scigraph/subclass/_search";
    URL obj = new URL(url);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

    String query = "{\"query\":{\"term\":{\"iri\":\"http://purl.obolibrary.org/obo/HP_0009283\"}}}";
    System.out.println(query);

    // optional default is GET
    con.setRequestMethod("GET");

    con.setDoOutput(true);
    byte[] outputInBytes = query.getBytes("UTF-8");
    OutputStream os = con.getOutputStream();
    os.write(outputInBytes);

    int responseCode = con.getResponseCode();
    System.out.println("\nSending 'GET' request to URL : " + url);
    System.out.println("Response Code : " + responseCode);

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();

    System.out.println(response.toString());
    Pattern pattern = Pattern.compile(".*\\[(.*?)\\].*");
    Matcher matcher = pattern.matcher(response.toString());
    boolean b = matcher.matches();
    String ids = matcher.group(1);
    System.out.println(ids);
    Result result2 =
        graphDb2.execute("MATCH path = (subclass)-[]-(connectedNode)-[:subClassOf*]->(superclass) WHERE id(subclass) IN [" + ids + "] RETURN path");
    stopwatch.stop(); // optional
    System.out.println("Finished. Elapsed time ==> " + stopwatch);
    System.out.println(Iterators.size(result2));
    tx2.success();
    
    // Finished. Elapsed time ==> 1.295 s
    // # results 269531088
  }
}
