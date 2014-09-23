import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.*;
import java.nio.channels.*;
import net.arnx.jsonic.JSON;
import java.math.BigDecimal;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.xml.sax.InputSource;

public class JarGet {

  public static List<String> opts = null;
  public static int rowCount = 100;

  public static String download(String _url){
    URL url = null;
    InputStream is = null;
    String ret = null;
    try{
      url = new URL(_url);
      is = url.openStream();
      ret = IOUtils.toString(is);
    }catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }finally{
      try{ is.close(); }catch(IOException e){ e.printStackTrace(); };
    }

    return ret;
  }

  public static byte[] downloadAsBytes(String _url){
    URL url = null;
    InputStream is = null;
    byte[] ret = null;
    try{
      url = new URL(_url);
      is = url.openStream();
      ret = IOUtils.toByteArray(is);
    }catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }finally{
      try{ is.close(); }catch(IOException e){ e.printStackTrace(); };
    }

    return ret;
  }

  public static void println(Object o){
    System.out.println(o);
  }

  public static void help(){
    println("usage: jarget [options] [args...]");
    println("");
    println("options:");
    println("    help");
    println("                --- show help");
    println("");
    println("    search [query]");
    println("                --- search jar with query");
    println("");
    println("    versions [group-id] [artifact]");
    println("                --- search versions of [group-id].[artifact]");
    println("");
    println("    install [group-id] [artifact] [version] (-d [directory])");
    println("                --- install [version] of [group-id].[artifact]");
    println("");
  }

  private static Document toXML(String xmlStr) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
      DocumentBuilder builder;  
      try{  
          builder = factory.newDocumentBuilder();  
          Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) ); 
          return doc;
      }catch (Exception e) {  
          e.printStackTrace();  
      } 
      return null;
  }

  public static String downloadFileAsString(String groupId, String artifact, String version, String ext){
    String id = groupId.replaceAll("\\.","/");
    String s = download("http://search.maven.org/remotecontent?filepath="+id+"/"+artifact+"/"+version+"/"+artifact+"-"+version+"."+ext);
    return s;
  }

  public static byte[] downloadFileAsBytes(String groupId, String artifact, String version, String ext){
    String id = groupId.replaceAll("\\.","/");
    byte[] s = downloadAsBytes("http://search.maven.org/remotecontent?filepath="+id+"/"+artifact+"/"+version+"/"+artifact+"-"+version+"."+ext);
    return s;
  }

  public static Document pom(String groupId, String artifact, String version){
    String s = downloadFileAsString(groupId, artifact, version, "pom");
    return toXML(s);
  }

  public static NodeList xpath(String xpathStr, Document node){
    try{
      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();
      XPathExpression xpathExpr = xpath.compile(xpathStr);
      return (NodeList)xpathExpr.evaluate(node, XPathConstants.NODESET);
    }catch(Exception e){
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  public static void install(String groupId, String artifact, String version){
    install(groupId, artifact, version, "");
  }

  public static void install(String groupId, String artifact, String version, String directory){
    println(String.format("\nInstalling %s:%s (%s)...\n", groupId, artifact, version));
    println("[Phase 1/4] Downloading Pom...");
    Document xml = pom(groupId, artifact, version);
    NodeList dependencies = xpath("//project/dependencies/dependency", xml);

    println("[Phase 2/4] Check Dependencies...\n");
    for(int i=0; i<dependencies.getLength(); i++){
      Element dependency = (Element)dependencies.item(i);
      String _groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
      String _artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
      String _version = dependency.getElementsByTagName("version").item(0).getTextContent();
      println(String.format("[Phase 3/4] Installing %s:%s (%s)...", _groupId, _artifactId, _version));

      byte[] = downloadFileAsBytes(_groupId, _artifactId, _version, "jar");
      
    }
  }

  public static void versions(String groupId, String artifact){
    String s = download("http://search.maven.org/solrsearch/select?q=g:%22"+groupId+"%22+AND+a:%22"+artifact+"%22&core=gav&rows="+rowCount+"&wt=json");
    Map<String,Object> result = JSON.decode(s);
    Map<String,Object> response = (Map<String,Object>)result.get("response");
    ArrayList<Map<String,Object>> docs = (ArrayList<Map<String,Object>>)response.get("docs");
    
    ArrayList<String> versions = new ArrayList<String>();
    for (Map<String,Object> doc : docs) {
      versions.add((String)doc.get("v"));
    }

    println("\nVersions:\n");

    for (String version : versions) {
      println(String.format("%s", version));
    }

    BigDecimal numFound = (BigDecimal)response.get("numFound");
    BigDecimal count = numFound.subtract(new BigDecimal(rowCount));
    if(count.compareTo(BigDecimal.ZERO) == 1){
      println("\n and more ... (rest: "+count+")");
    }
  }

  public static void search(String query){
    String enQuery = URLEncoder.encode(query);
    String s = download("http://search.maven.org/solrsearch/select?q="+enQuery+"&rows="+rowCount+"&wt=json");
    Map<String,Object> result = JSON.decode(s);
    Map<String,Object> response = (Map<String,Object>)result.get("response");
    ArrayList<Map<String,Object>> docs = (ArrayList<Map<String,Object>>)response.get("docs");
    
    ArrayList<String> groups = new ArrayList<String>();
    ArrayList<String> artifacts = new ArrayList<String>();
    for (Map<String,Object> doc : docs) {
      groups.add((String)doc.get("g"));
      artifacts.add((String)doc.get("a"));
    }

    println("\nSearch Results:\n");

    for (int i=0; i<groups.size(); i++) {
      String group = groups.get(i);
      String artifact = artifacts.get(i);

      println(String.format("%s (%s)", artifact, group));
    }

    BigDecimal numFound = (BigDecimal)response.get("numFound");
    BigDecimal count = numFound.subtract(new BigDecimal(rowCount));
    if(count.compareTo(BigDecimal.ZERO) == 1){
      println("\n and more ... (rest: "+count+")");
    }
  }

  public static void main(String[] args) {
    opts = Arrays.asList(args);
    if(opts.size() == 0 || opts.get(0).equals("help")){
      help();
    }else if(opts.size() == 2 && opts.get(0).equals("search")){
      search(opts.get(1));
    }else if(opts.size() == 3 && opts.get(0).equals("versions")){
      versions(opts.get(1), opts.get(2));
    }else if(opts.size() == 4 && opts.get(0).equals("install")){
      install(opts.get(1), opts.get(2), opts.get(3));
    }else if(opts.size() == 6 && opts.get(0).equals("install") && opts.get(4).equals("-d")){
      install(opts.get(1), opts.get(2), opts.get(3), opts.get(5));
    }else{
      help();
    }
  }
}