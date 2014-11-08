package com.gear2cam.official;

/**
 * Created by varun on 2/6/14.
 */
public class Quadrant {
    int angle;
    int defaultOrientation;

    public static final int ORIENTATION_PORTRAIT = 1;
    public static final int ORIENTATION_LANDSCAPE = 2;
    public static final int ORIENTATION_PORTRAIT_INVERSE = 3;
    public static final int ORIENTATION_LANDSCAPE_INVERSE = 4;

    public int getOrientation() {
        if(defaultOrientation == 1) {
            //Portrait (native)
            int quadrant = getQuadrantNumber(angle);
            if(quadrant == 0) {
                return ORIENTATION_PORTRAIT;
            }
            if (quadrant == 1) {
                return ORIENTATION_LANDSCAPE_INVERSE;
            }
            if(quadrant == 2) {
                return ORIENTATION_PORTRAIT_INVERSE;
            }
            if(quadrant == 3) {
                return ORIENTATION_LANDSCAPE;
            }
        }
        if(defaultOrientation == 2) {
            //Landscape (native)
            int quadrant = getQuadrantNumber(angle);
            if(quadrant == 0) {
                return ORIENTATION_LANDSCAPE;
            }
            if (quadrant == 1) {
                return ORIENTATION_PORTRAIT;
            }
            if(quadrant == 2) {
                return ORIENTATION_LANDSCAPE_INVERSE;
            }
            if(quadrant == 3) {
                return ORIENTATION_PORTRAIT_INVERSE;
            }
        }
        return 0;
    }

    public int getRotation() {
        if(defaultOrientation == 1) {
            //Portrait (native)
            int quadrant = getQuadrantNumber(angle);
            if(quadrant == 0) {
                return 270;
            }
            if (quadrant == 1) {
                return 180;
            }
            if(quadrant == 2) {
                return 90;
            }
            if(quadrant == 3) {
                return 0;
            }
        }
        if(defaultOrientation == 2) {
            //Landscape (native)
            int quadrant = getQuadrantNumber(angle);
            if(quadrant == 0) {
                return 0;
            }
            if (quadrant == 1) {
                return 270;
            }
            if(quadrant == 2) {
                return 180;
            }
            if(quadrant == 3) {
                return 90;
            }
        }
        return 0;
    }

    public static int getQuadrantNumber(int angle) {
        if(angle>=315 || angle<45) {
            return 0;
        }
        else if(angle>=45 && angle < 135) {
            return 1;
        }
        else if(angle>=135 && angle < 225) {
            return 2;
        }
        else if(angle>=225 && angle < 315) {
            return 3;
        }
        else {
            return -1;
        }
    }


    public Quadrant(int angle, int defaultOrientation) throws Exception {
        if(angle < 0 || angle > 360) {
            throw  new Exception("Invalid angle:" + angle);
        }
        if(defaultOrientation< 1 || defaultOrientation > 2) {
            throw  new Exception("Invalid orientation:" + defaultOrientation);
        }

        this.defaultOrientation = defaultOrientation;
        this.angle = angle;
    }
}
