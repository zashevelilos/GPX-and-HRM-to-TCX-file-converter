package com.company;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//        The program merges the polar xpm file and gpx file into a tcx file. To do this,
//        you must enter the ABSOLUTE path to the directory in which there are pairs
//        of hrm and gpx files with the same name. The result of the execution will
//        be TCX file with the same name in the same directory.


public class Main {

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        BufferedReader is = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Directory of GPX and HRM files: ");
        String directory=is.readLine();

        HashMap<String,String> map = getallgpxhrmfilesindirectory(directory+"\\");
        System.out.println(directory+"\\");


        for (String hrm:map.keySet()){
            System.out.println("Find the pair: " + hrm+" + "+map.get(hrm));
            String resultfile=map.get(hrm).split(".gpx",2)[0]+".tcx";
            System.out.println("Result will be: " + resultfile);
            MergeGPXHRM(hrm,map.get(hrm),resultfile);
            System.out.println();
        }


        System.out.println();


    }

    //finds all pairs of hrm+gpx files with same name for split and return a hashmap with detected pairs
    private static HashMap<String, String> getallgpxhrmfilesindirectory(String directory){
        File file = new File(directory);
        String[] listofallfiles=file.list();//all files in directory

        HashMap<String,String> map = new HashMap<>();//all pairs of gpx+hrm files with same name
        for (int i=0;i<listofallfiles.length;i++) {
            if (listofallfiles[i].toLowerCase().endsWith(".gpx")){
                if(new File(directory+listofallfiles[i].split(".gpx",2)[0]+".hrm").exists()){
                    map.put(directory+listofallfiles[i].split(".gpx",2)[0]+".hrm",directory+listofallfiles[i]);
                }
            }
        }
        return map;
    }

    //this method merge polar hrm file and gpx file to one tcx file
    //hrmfile - absolute path to hrm file
    //gpxfile - absolute path to gpx file
    //absolute path to tcx(result) file
    private static void MergeGPXHRM(String hrmfile,String gpxfile,String tcxfile) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File(gpxfile));

        NodeList GPX_trkpt_elements = document.getDocumentElement().getElementsByTagName("trkpt");

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(hrmfile))));
        String s;
        String h;
        int second=0;


        int countofGPXpoints=0;
        ArrayList<waypoint> ArrayListOfWaypoint = new ArrayList<>();

        for (int i=0; i<GPX_trkpt_elements.getLength();i++){
            countofGPXpoints++;
            waypoint w = new waypoint();

            Node place = GPX_trkpt_elements.item(i);
            NamedNodeMap attributes = place.getAttributes();

            NodeList time = place.getChildNodes();

            String s1 = time.item(1).getTextContent();
            Pattern p = Pattern.compile("(.*)");
            Matcher m = p.matcher(s1);
            m.find();

            w.date=m.group(1);


            s1 = attributes.getNamedItem("lat").toString();
            p=Pattern.compile("\"(.*)\"");
            m=p.matcher(s1);
            m.find();
            w.lat=m.group(1);

            s1 = attributes.getNamedItem("lon").toString();
            p=Pattern.compile("\"(.*)\"");
            m=p.matcher(s1);
            m.find();
            w.lon=m.group(1);


            ArrayListOfWaypoint.add(w);
        }


        while((s=br.readLine())!=null){
            if (s.equals("[HRData]")){
                while((h=br.readLine())!=null){
                    if (ArrayListOfWaypoint.size()>second){
                        Pattern pat= Pattern.compile("^(\\d*)");
                        Matcher mat = pat.matcher(h);
                        mat.find();

                        waypoint w = new waypoint();
                        w.lat=ArrayListOfWaypoint.get(second).lat;
                        w.lon=ArrayListOfWaypoint.get(second).lon;
                        w.date=ArrayListOfWaypoint.get(second).date;
                        w.hrm=mat.group(1);

                        ArrayListOfWaypoint.set(second,w);

                        second++;
                    }}
            }
        }

        System.out.println("Training, minutes:" + (float)countofGPXpoints/60);




        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();      //Создаем документ

        Element trainingcenterdatabasenode =doc.createElement("TrainingCenterDatabase");

        trainingcenterdatabasenode.setAttribute("xmlns","http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2");
        doc.appendChild(trainingcenterdatabasenode);


        Element activitiesnode =doc.createElement("Activities");

        Element activitynode = doc.createElement("Activity");
        activitynode.setAttribute("Sport", "Running");
        activitiesnode.appendChild(activitynode);
        trainingcenterdatabasenode.appendChild(activitiesnode);


        Element idnode = doc.createElement("Id");

        Element lapnode = doc.createElement("Lap");
        lapnode.setAttribute("StartTime", ArrayListOfWaypoint.get(0).date);


        activitynode.appendChild(idnode);
        activitynode.appendChild(lapnode);
        idnode.appendChild(doc.createTextNode(ArrayListOfWaypoint.get(0).date));

        Element tracknode = doc.createElement("Track");

        for(waypoint wayp:ArrayListOfWaypoint){
            Element trackpointnode = doc.createElement("Trackpoint");
            Element timenode = doc.createElement("Time");
            timenode.appendChild(doc.createTextNode(wayp.date));
            trackpointnode.appendChild(timenode);
            Element positionnode = doc.createElement("Position");
            trackpointnode.appendChild(positionnode);
            Element latitudedegreesnode = doc.createElement("LatitudeDegrees");
            Element longitudedegreesnode = doc.createElement("LongitudeDegrees");
            positionnode.appendChild(latitudedegreesnode);
            positionnode.appendChild(longitudedegreesnode);
            latitudedegreesnode.appendChild(doc.createTextNode(wayp.lat));
            longitudedegreesnode.appendChild(doc.createTextNode(wayp.lon));
            Element heartratebpmnode = doc.createElement("HeartRateBpm");
            trackpointnode.appendChild(heartratebpmnode);
            Element valuenode = doc.createElement("Value");
            heartratebpmnode.appendChild(valuenode);
            valuenode.appendChild(doc.createTextNode(wayp.hrm));
            Element sensorstatenode = doc.createElement("SensorState");
            sensorstatenode.appendChild(doc.createTextNode("Present"));
            positionnode.appendChild(sensorstatenode);
            tracknode.appendChild(trackpointnode);

        }
        lapnode.appendChild(tracknode);

        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(tcxfile)));
        br.close();
    }

}
