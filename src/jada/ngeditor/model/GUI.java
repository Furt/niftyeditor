/* Copyright 2012 Aguzzi Cristiano

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package jada.ngeditor.model;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.window.WindowControl;
import jada.ngeditor.listeners.actions.Action;
import jada.ngeditor.model.elements.GElement;
import jada.ngeditor.model.elements.GLayer;
import jada.ngeditor.model.elements.GScreen;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Observable;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Main model class it is a container for all GUI elements
 * @author cris
 */
public class GUI extends Observable{
    private static int GUIID = 0;
    private final Nifty manager;
    
    private LinkedList<GScreen> screens;
    private LinkedList<GLayer> currentlayers;
    
    private GScreen currentS;
    private GLayer  currentL;
    private GElement selected;
    private static Document document;
    private  Element root;
    
    public static Element elementFactory(String tag){
        if(document!=null)
            return document.createElement(tag);
        return null;
    }
    
    /**
     * Creates a new gui
     * @param nifty 
     */
    protected GUI(Nifty nifty) throws ParserConfigurationException{
       this.manager = nifty;
       this.screens = new LinkedList<GScreen> ();
       this.currentlayers = new LinkedList<GLayer> ();
       
       this.currentS = null;  
       this.currentL = null;
       document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
       root = document.createElement("nifty");
       Element style = document.createElement("useStyles");
       Element controls = document.createElement("useControls");
       
       style.setAttribute("filename", "nifty-default-styles.xml");
       controls.setAttribute("filename", "nifty-default-controls.xml");
       
       document.appendChild(root);
       root.appendChild(style);
       root.appendChild(controls);
       this.GUIID++;
       
    }
    
    protected GUI(Nifty nifty,Document doc){
        this.manager = nifty;
       this.screens = new LinkedList<GScreen> ();
       this.currentlayers = new LinkedList<GLayer> ();
       this.currentS = null;  
       this.currentL = null;
       document = doc;
       root = (Element) document.getElementsByTagName("nifty").item(0);
       this.GUIID++;
    }
    
    public DOMSource getSource(){
        return new DOMSource(document);
    }
    
    public void addScreen(GScreen screen){
       this.screens.add(screen);
       root.appendChild(screen.toXml());
       screen.createNiftyElement(manager);
       manager.gotoScreen(screen.getID());
    }
    /**
     * Creates a new gui from a specific file
     * @param nifty
     * @param res 
     */
    public GUI(Nifty nifty, String res){
        this.manager=nifty;
        
    }
    
   
    public LinkedList<GScreen> getScreens(){
        return this.screens;
    }
    public void addElementToParent(GElement child,GElement parent){
        if(parent == null){
        GScreen screen = (GScreen) child;
        this.screens.add(screen);
        this.currentS=screen;
        root.appendChild(screen.toXml());
        }
        else if(parent.getType().equals(Types.SCREEN)){
            GLayer temp =(GLayer) child;
            this.currentL=temp;
            this.currentlayers.add(temp);
            if(this.currentS != null)
                parent.addChild(child, false);
        } else 
            parent.addChild(child, false);
        this.setChanged();
        this.notifyObservers(new Action(Action.ADD,child));
        this.clearChanged();
            
    }
    public boolean addElement(GElement child,GElement parent){
         parent.addChild(child, true);
         child.createNiftyElement(manager);
         return true;
    }
    
   
    
    public void move(Point2D to,GElement toEle, GElement from){
        if(!toEle.equals(from)){
          de.lessvoid.nifty.elements.Element nTo ;
           if(toEle.getType().equals(Types.WINDOW)){
                nTo = toEle.getNiftyElement().getNiftyControl(WindowControl.class).getContent();
           }else
               nTo = toEle.getNiftyElement();
        if(toEle.getAttribute("childLayout").equals("absolute")){
            int parentX = toEle.getNiftyElement().getX();
                int parentY = toEle.getNiftyElement().getY();
                       
                from.addAttribute("x",""+ (int)(to.getX()-parentX)); 
                from.addAttribute("y",""+ (int)(to.getY()-parentY));
            if(!from.getParent().equals(toEle)){
                this.manager.moveElement(this.manager.getCurrentScreen(), from.getNiftyElement(), nTo, null);
            }
            from.lightRefresh();
        }else 
            this.manager.moveElement(this.manager.getCurrentScreen(), from.getNiftyElement(), nTo, null);
            from.removeFromParent();
            toEle.addChild(from, true);
            
        }
      }

   
    public void removeElement(GElement e){
        if(e.getType().equals(Types.SCREEN)){
            this.screens.remove(e);
            manager.removeScreen(e.getID());
        }
        else if(e.getType().equals(Types.LAYER)){
            this.currentlayers.remove(e);
            manager.removeElement(manager.getCurrentScreen(), e.getNiftyElement());
        }
        else
            manager.removeElement(manager.getCurrentScreen(), e.getNiftyElement());
        e.removeFromParent();
    }
    
   public void reloadAfterFresh(){
        for(GScreen sel : this.screens)
            sel.reloadElement(manager);
        for(GLayer lay : this.currentlayers){
            lay.reloadElement(manager);
            for(GElement ele : this.getAllChild(lay))
                ele.reloadElement(manager);
        }
    }
    public LinkedList<GElement> getAllChild(GElement element){
         LinkedList<GElement> res = new LinkedList<GElement>();
         for(GElement ele : element.getElements()){
               res.add(ele);
               res.addAll(getAllChild(ele));
          }
         return res;
    }
    
    public GScreen gettopScreen(){
        return this.screens.peekLast();
    }
    
  
    public void goTo(GScreen screen){
        this.manager.gotoScreen(screen.getID());
    }
    @Override
    public String toString(){
        return ""+this.GUIID;
    }
    
    public GLayer getTopLayer(){
        return this.currentlayers.peekLast();
    }
}