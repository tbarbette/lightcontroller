package be.itstudents.tom.android.lightcontroller;

/**
 * Interface representing any Lighting device (which allows to access multiple bulbs)
 */
public interface LightDevice {
    /**
     * Set the intensity of a light
     *
     * @param id        Index of light
     * @param intensity Number between 0 and 256 indicating the light
     */
    void setLight(int id, int intensity);

    /**
     * Open the device. The device has to send a MESSAGE_LIST_ITEMS as soon as possible after this call.
     *
     * @param client_id Id of the cliebt
     * @return True if all went okay
     */
    boolean open(int client_id);

    /**
     * Specify that this device is unselected
     */
    void close();

    /**
     * Get device name
     */
    String getName();
}
