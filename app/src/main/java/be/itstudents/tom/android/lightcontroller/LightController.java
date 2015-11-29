package be.itstudents.tom.android.lightcontroller;

/**
 * Interface for a light controller, the light controller allows to search for LightDevice.
 */
public interface LightController {
    public static final int MESSAGE_RESPONSE = 2;
    public static final int MESSAGE_ADD_DEVICES = 3;
    public static final int MESSAGE_REMOVE_DEVICES = 4;
    public static final int MESSAGE_LIST_ITEMS = 5;

    /**
     * Launch the search for new devices
     *
     * @return true if all went okay
     */
    boolean search();


    /**
     * Set the controller on pause
     */
    void pause();
}
