package com.shpp.p2p.cs.obaskakov.assignment12;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * A program that counts and outputs to the console the number of silhouettes
 * of large objects in the picture.
 * The algorithm used to solve the problem is recursive depth-first search.
 * The name of the image file comes as the first parameter at the input of the program.
 * If there are no parameters, the filename is assumed to be 'test.jpg'.
 * The program should take into account only large objects.
 * It is assumed that the background on which the objects are located is light, and the objects themselves are dark.
 * The recursive method may require a stack of huge size for its execution, which can lead to stack overflow.
 * The size of the stack in the main thread is set by default in the JVM.
 * To increase its size in this program, an additional thread is launched,
 * and in the constructor of that thread, the fourth parameter can be used
 * to set the size of the stack for that thread.
 */
public class Assignment12Part1 {

    /**
     * Input point of the program.
     *
     * @param arg - arg[0] is the name of image file, if any.
     */
    public static void main(String[] arg) {
        System.out.println("Starting the main thread");

        String fnm;
        if (arg.length == 0) {
            fnm = "test.jpg";
        } else {
            fnm = arg[0];
        }

        // Additional thread for calculation of the number of objects.
        ImageProcessing mw = new ImageProcessing(fnm);
        mw.start();

        // The main thread is waiting for the secondary thread to complete.
        try {
            mw.join();
        } catch (InterruptedException e) {
            System.out.println("Something went wrong");
        }
        System.out.println("Task completed");
    }

}

/**
 * A class that organizes the second thread and counts large objects in the picture.
 */
class ImageProcessing extends Thread {
    // Stack size.
    final private static long STACK_SIZE = 10_000_000L;
    // The maximum brightness of the pixels of the objects being searched for. From the range 0 - 255.
    final private static int COLOR_OF_EDGE = 10;
    // The maximum relative size of small objects that should be excluded from the count.
    final private static double MAX_SMALL_OBJECT_SIZE = 0.01;
    // Directions along the x and y axes to the nearest pixels from the current pixel.
    final private static Integer[][] PATHS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    // Image file name.
    private final String fileName;
    // BufferedImage to store image.
    private BufferedImage image = null;
    // An array of boolean values that indicates, for each pixel, whether it has been visited.
    private boolean[][] visited;
    // An array of object numbers for each pixel indicating which object the pixel belongs to.
    // Background pixels have number 0.
    private Integer[][] objectNumbers;
    // Number of the current object.
    private int currentObjNumber = 0;

    /**
     * Constructor that sets stack size.
     *
     * @param fnm name of the image file.
     */
    public ImageProcessing(String fnm) {
        super(null, null, "Image processing", STACK_SIZE);
        fileName = fnm;
    }

    /**
     * The method of interface Runnable.
     * It contains a general thread algorithm.
     */
    public void run() {
        System.out.println("Starting the second thread");
        System.out.println("The name of image file " + fileName);

        // Reading an image from a file.
        readImage(fileName);

        // Calculation of the number of objects.
        System.out.println("Total number of large objects is " + findSilhouettes());
    }

    /**
     * Reading an image from a file.
     *
     * @param fileName name of the file.
     */
    private void readImage(String fileName) {
        try {
            image = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method with general algorithm.
     *
     * @return number of detected large objects.
     */
    public int findSilhouettes() {

        // Array initialization
        initArrays();

        // Realization of the recursive depth-first search.
        lookingForObjects();

        if (currentObjNumber != 0) {
            int[] np = numbPixInObj();
            // Dropping small objects.
            return smallObjDel(np, image.getHeight() * image.getWidth());
        }
        return 0;
    }

    /**
     * Array initialization.
     */
    private void initArrays() {
        int width = image.getWidth();
        int height = image.getHeight();
        visited = new boolean[width][height];  // all elements are false.
        objectNumbers = new Integer[width][height];  // all elements are null;
        for (int l = 0; l < height; l++) {
            for (int k = 0; k < width; k++) {
                objectNumbers[k][l] = 0;  // all elements are 0;
            }
        }

        System.out.println("Image width: " + width);
        System.out.println("Image height: " + height);
    }

    /**
     * Realization of the recursive depth-first search.
     */
    private void lookingForObjects() {
        // Loop over all graph nodes (pixels).
        // Node is a pixel.
        // // Node is connected to nearest nodes located at distances
        // abs(dx) <=0 and abs(dy) <= 0.
        // To make this pixel a vertex of a new object graph, it is necessary to ensure
        // that it has not been visited before and that it does not belong to a known object.
        for (int c = 0; c < image.getHeight(); c++) {
            for (int l = 0; l < image.getWidth(); l++) {
                if (!visited[l][c] && isObjectPix(l, c)) {
                    currentObjNumber++;

                    // Recursive function.
                    dfs(l, c);
                }
            }
        }
        System.out.println("Number of objects identified is " + currentObjNumber);
    }

    /**
     * Recursive function of depth-first search algorithm.
     * @param x - coordinate of the pixel.
     * @param y - coordinate of the pixel.
     */
    private void dfs(int x, int y) {
        if (x >= image.getWidth() || x < 0) {
            return;
        }
        if (y >= image.getHeight() || y < 0) {
            return;
        }
        if (!isObjectPix(x, y)) {
            return;
        }
        if (visited[x][y]) {
            return;
        }
        visited[x][y] = true;
        objectNumbers[x][y] = currentObjNumber;

        for (var p : PATHS) {
            int x1 = x + p[0];
            int y1 = y + p[1];
            dfs(x1, y1);
        }
    }

    /**
     * Does pixel belongs to the object?
     * @param x - coordinate of the pixel.
     * @param y - coordinate of the pixel.
     * @return true if yes.
     */
    private boolean isObjectPix(int x, int y) {
        return (avrgPixColor(x, y) <= COLOR_OF_EDGE);
    }

    /**
     * Determination of the pixel brightness.
     * @param x - coordinate of the pixel.
     * @param y - coordinate of the pixel.
     * @return result.
     */
    private int avrgPixColor(int x, int y) {
        Color c = new Color(image.getRGB(x, y));
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        return (r + g + b) / 3;
    }

    /**
     * Calculation of the quantity of pixels for all objects.
     * @return - array of quantities.
     */
    private int[] numbPixInObj() {
        int[] np = new int[currentObjNumber];
        Arrays.fill(np, 0);

        for (int k = 0; k < image.getHeight(); k++) {
            for (int l = 0; l < image.getWidth(); l++) {
                if (objectNumbers[l][k] != 0) {
                    np[objectNumbers[l][k] - 1]++;
                }
            }
        }
        return np;
    }

    /**
     * Dropping small objects.
     *
     * @param np     array of pixel quantities of objects.
     * @param totPix total number of pixels in image.
     * @return number of large objects.
     */
    private int smallObjDel(int[] np, int totPix) {
        int numbEssObj = 0;
        for (var k : np) {
            if ((double) k / totPix > MAX_SMALL_OBJECT_SIZE) {
                numbEssObj++;
            }
        }
        return numbEssObj;
    }

}
