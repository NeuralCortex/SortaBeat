package com.nc.sortabeat;

import java.util.Locale;

/**
 *
 * @author Neural Cortex
 */
public class Globals {

    public static final Locale DEFAULT_LOCALE = Locale.US;

    public static final double HEIGHT = 400;
    public static final double WIDTH = HEIGHT * 18.0f / 9.0f;

    public static final boolean MAXIMIZED = false;

    public static final String BUNDLE_PATH = "com.nc.sortabeat.bundle.sorta";
    public static final String LOG4J2_CONFIG_PATH = System.getProperty("user.dir") + "/config/log4j2.xml";
    public static final String XML_CONFIG_PATH = System.getProperty("user.dir") + "/config/config.xml";

    //Images
    public static final String APP_LOGO_PATH = System.getProperty("user.dir") + "/images/kdf.png";
    
    public static final String PNG_PLAY = "/images/play.png";
    public static final String PNG_STOP = "/images/stop.png";
    public static final String PNG_PAUSE = "/images/pause.png";
    public static final String PNG_PREVIOUS = "/images/prev.png";
    public static final String PNG_NEXT = "/images/next.png";
    public static final String PNG_OPEN = "/images/open.png";
    public static final String PNG_LOGO = "/images/logo.png";
    public static final String PNG_ADD = "/images/add.png";
    public static final String PNG_REVERT = "/images/revert.png";
    public static final String PNG_SAVE_AS = "/images/saveas.png";

    public static final String CSS_PATH = "/com/nc/sortabeat/style/sorta.css";

    //FXML
    public static final String FXML_PATH = "/com/nc/sortabeat/fxml/";

    public static final String FXML_MAIN_PATH = FXML_PATH + "main_app.fxml";

    public static XMLPropertyManager propman;

    static {
        propman = new XMLPropertyManager(XML_CONFIG_PATH);
    }

    //Config
    public static final String OPEN_DIR_PATH = "OPEN_DIR_PATH";
    public static final String SAVE_AS_DIR_PATH = "SAVE_AS_DIR_PATH";
    public static final String VOLUME = "VOLUME";
    public static final String PREFIX_LENGTH = "PREFIX_LENGTH";
    public static final String SCROLL_SPEED = "SCROLL_SPEED";
    
    public static final String WIN_X = "WIN_X";
    public static final String WIN_Y = "WIN_Y";
    public static final String WIN_WIDTH = "WIN_WIDTH";
    public static final String WIN_HEIGHT = "WIN_HEIGHT";
}
