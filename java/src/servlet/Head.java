package servlet;

/**
 * The head of an HTML page.
 */
public class Head extends Node {
    String title;
    String styleFile;
    
    /** Create a page head. Non-public to allow servlet to control
     *  creation through the createHead() method.
     * @param t The page title
     * @param styleFile The URL of the style file. May be null to signal absence.
     */	 
    Head(String t, String s) {
        title = t;
        styleFile = s;
    }
    
    public void write(HTMLWriter w) {
        w.begin(2);
        w.print("<head>");
        w.breakLine();
        w.print("<title>");
        w.escape(title);
        w.print("</title>");
        
        if (styleFile != null) {
            w.breakLine();
            w.print("<link ");
            w.begin();
            w.print("rel=\"stylesheet\" type=\"text/css\" href=");
            w.printq(styleFile);
            w.print(" type=\"text/css\" />");
            w.end();
        }
        w.end();
        w.breakLine();
        w.print("</head>");
        w.breakLine();
    }
}
