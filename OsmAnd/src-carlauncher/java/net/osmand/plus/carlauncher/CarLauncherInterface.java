package net.osmand.plus.carlauncher;

/**
 * Interface to expose CarLauncher specific methods from MapActivity.
 * Used to avoid compilation errors due to MapActivity shadowing between source
 * sets.
 */
public interface CarLauncherInterface {
    void openAppDrawer();

    void closeAppDrawer();
}
