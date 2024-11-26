package org.firstinspires.ftc.robotcontroller;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.TouchSensor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.BuiltinCameraDirection;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvPipeline;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.SimpleBlobDetector;
import org.opencv.features2d.SimpleBlobDetector_Params;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.OpenCVLoader;

import java.util.List;

@TeleOp(name = "teleop")
public class teleop extends LinearOpMode {
    TouchSensor button;
    public DcMotor FL;
    public DcMotor BL;
    public DcMotor FR;
    public DcMotor BR;
    public DcMotor elevation;
    public DcMotor slide;
    public CRServo roller;
    public Servo tilt;
    public Servo dump;
    // public Servo launch;


    final float normalPower = 0.7f;
    final float lowerPower = 0.4f;

    private static final boolean USE_WEBCAM = true;  // true for webcam, false for phone camera
    private AprilTagProcessor aprilTag;
    private SimpleBlobDetector blobDetector;
    private VisionPortal visionPortal;
    private OpenCvCamera camera;

    @Override
    public void runOpMode() {
        float turn_FL_X = 0;
        float turn_BR_X = 0;
        float turn_FR_X = 0;
        float turn_BL_X = 0;
        float strafe_FR_X = 0;
        float strafe_BL_X = 0;
        float strafe_BR_X = 0;
        float strafe_FL_X = 0;
        float strafe_FL_Y = 0;
        float strafe_FR_Y = 0;
        float strafe_BL_Y = 0;
        float strafe_BR_Y = 0;
        double driveSpeed = 1.0;
        int evelation_hold_pos;
        boolean elevation_locked = false;
        long lock_start_time = System.currentTimeMillis();
        boolean buttonHitRecently = false;
        long buttonHitTime = 0;
        int HOLD_DURATION = 700;
        double HOLDING_POWER = 0.2;
        boolean isPositionSet = false;
        long positionHoldStartTime = 0;
        boolean unload_on_button_lock = false;
        long unroll_start_time = 0;
        boolean sequenceStarted = false;
        boolean slide_set = false;

        FL = hardwareMap.get(DcMotor.class, "leftfront");
        BL = hardwareMap.get(DcMotor.class, "leftback");
        FR = hardwareMap.get(DcMotor.class, "rightfront");
        BR = hardwareMap.get(DcMotor.class, "rightback");
        elevation = hardwareMap.get(DcMotor.class, "elevationMotor");
        slide = hardwareMap.get(DcMotor.class, "slideMotor");
        tilt = hardwareMap.get(Servo.class, "tilt");
        roller = hardwareMap.get(CRServo.class, "roller");
        dump = hardwareMap.get(Servo.class, "dump");
        // launch = hardwareMap.get(Servo.class, "launch");


        button = hardwareMap.get(TouchSensor.class, "button");

        initAprilTag();
        waitForStart();
        if (opModeIsActive()) {

            elevation.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            slide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            FL.setDirection(DcMotorSimple.Direction.FORWARD);
            BL.setDirection(DcMotorSimple.Direction.FORWARD);
            FR.setDirection(DcMotorSimple.Direction.REVERSE);
            BR.setDirection(DcMotorSimple.Direction.REVERSE);
            FL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            FR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            BL.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            BR.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

            evelation_hold_pos = elevation.getCurrentPosition(); // bad code lol
            visionPortal.resumeStreaming();

            while (opModeIsActive()) {
                telemetry.addData("leftstickX", gamepad1.left_stick_x);
                telemetry.addData("leftstickY", gamepad1.left_stick_y);
                telemetry.addData("rightstickX", gamepad1.right_stick_x);
                // telemetry.update();
                //lower power for more precise robot movement
              /*  if (gamepad1.x) {

                    if (gamepad1.right_stick_y > 0.1) {
                        // forward
                        strafe_BR_Y = gamepad1.right_stick_y * lowerPower;
                        strafe_FL_Y = gamepad1.right_stick_y * lowerPower;
                        strafe_FR_Y = gamepad1.right_stick_y * lowerPower;
                        strafe_BL_Y = gamepad1.right_stick_y * lowerPower;
                    } else if (gamepad1.right_stick_y < -0.1) {
                        // backward
                        strafe_BR_Y = gamepad1.right_stick_y * lowerPower;
                        strafe_FL_Y = gamepad1.right_stick_y * lowerPower;
                        strafe_FR_Y = gamepad1.right_stick_y * lowerPower;
                        strafe_BL_Y = gamepad1.right_stick_y * lowerPower;
                    } else if (gamepad1.left_stick_x > 0.1) {
                        // left turn
                        turn_FL_X = -gamepad1.left_stick_x * lowerPower;
                        turn_FR_X = gamepad1.left_stick_x * lowerPower;
                        turn_BL_X = -gamepad1.left_stick_x * lowerPower;
                        turn_BR_X = gamepad1.left_stick_x * lowerPower;
                    } else if (gamepad1.left_stick_x < -0.1) {
                        // right turn
                        turn_FL_X = -gamepad1.left_stick_x * lowerPower;
                        turn_FR_X = gamepad1.left_stick_x * lowerPower;
                        turn_BL_X = -gamepad1.left_stick_x * lowerPower;
                        turn_BR_X = gamepad1.left_stick_x * lowerPower;
                    } else {
                        turn_FL_X = 0;
                        turn_FR_X = 0;
                        turn_BL_X = 0;
                        turn_BR_X = 0;
                        strafe_FL_Y = 0;
                        strafe_FR_Y = 0;
                        strafe_BL_Y = 0;
                        strafe_BR_Y = 0;

                    }
                } else { */
                if (gamepad1.left_stick_y > 0.1) {
                    // forward
                    strafe_BR_Y = gamepad1.left_stick_y * normalPower;
                    strafe_FL_Y = gamepad1.left_stick_y * normalPower;
                    strafe_FR_Y = gamepad1.left_stick_y * normalPower;
                    strafe_BL_Y = gamepad1.left_stick_y * normalPower;
                } else if (gamepad1.left_stick_y < -0.1) {
                    // backward
                    strafe_BR_Y = gamepad1.left_stick_y * normalPower;
                    strafe_FL_Y = gamepad1.left_stick_y * normalPower;
                    strafe_FR_Y = gamepad1.left_stick_y * normalPower;
                    strafe_BL_Y = gamepad1.left_stick_y * normalPower;
                } else if (gamepad1.right_stick_x > 0.1) {
                    // left turn
                    turn_FL_X = -gamepad1.right_stick_x * normalPower;
                    turn_FR_X = gamepad1.right_stick_x * normalPower;
                    turn_BL_X = -gamepad1.right_stick_x * normalPower;
                    turn_BR_X = gamepad1.right_stick_x * normalPower;
                } else if (gamepad1.right_stick_x < -0.1) {
                    // right turn
                    turn_FL_X = -gamepad1.right_stick_x * normalPower;
                    turn_FR_X = gamepad1.right_stick_x * normalPower;
                    turn_BL_X = -gamepad1.right_stick_x * normalPower;
                    turn_BR_X = gamepad1.right_stick_x * normalPower;
                } else {
                    turn_FL_X = 0;
                    turn_FR_X = 0;
                    turn_BL_X = 0;
                    turn_BR_X = 0;
                    strafe_FL_Y = 0;
                    strafe_FR_Y = 0;
                    strafe_BL_Y = 0;
                    strafe_BR_Y = 0;

                }

                // strafe
                if (gamepad1.left_stick_x < -0.1) {
                    // right strafe
                    strafe_FL_X = -gamepad1.left_stick_x;
                    strafe_FR_X = gamepad1.left_stick_x;
                    strafe_BL_X = gamepad1.left_stick_x;
                    strafe_BR_X = -gamepad1.left_stick_x;
                } else if (gamepad1.left_stick_x > 0.1) {
                    // left strafe
                    strafe_FL_X = -gamepad1.left_stick_x;
                    strafe_FR_X = gamepad1.left_stick_x;
                    strafe_BL_X = gamepad1.left_stick_x;
                    strafe_BR_X = -gamepad1.left_stick_x;
                } else {
                    strafe_FL_X = 0;
                    strafe_FR_X = 0;
                    strafe_BL_X = 0;
                    strafe_BR_X = 0;
                }

                // elevation (formerly pitch)
                if (gamepad1.left_bumper) {
                    // Move down
                    elevation.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    elevation.setPower(1); // Assuming positive power moves it down
                    elevation_locked = false; // Do not hold position
                } else if (gamepad1.right_bumper) {
                    // Move up
                    if (tilt.getPosition() <= 0.4) {
                        tilt.setPosition(0.4);
                        sleep(50); // Prevent rapid re-positioning
                    }
                    evelation_hold_pos = elevation.getCurrentPosition(); // Save hold position
                    elevation.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    elevation.setPower(-1); // Assuming negative power moves it up
                    lock_start_time = System.currentTimeMillis(); // Timer to avoid overuse
                    elevation_locked = true; // Enable holding position
                } else {
                    // No button pressed
                    if (elevation_locked && (System.currentTimeMillis() - lock_start_time < 5000)) {
                        // Hold the last position if within safe locking time
                        elevation.setTargetPosition(evelation_hold_pos);
                        elevation.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        elevation.setPower(0.2); // Small power to maintain position
                    } else {
                        // If lock expired or no lock, stop movement
                        elevation.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        elevation.setPower(0);
                        elevation_locked = false;
                    }
                }


                if (gamepad1.dpad_up) {                 // limit horizontal position

                    telemetry.addData("Horizontal Slide Encoder", slide.getCurrentPosition());
                    if (slide.getCurrentPosition() >= -1735) {
                        // Manual control moving out
                        slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        slide.setPower(-1);
                        isPositionSet = false;
                    } else {
                        slide.setTargetPosition(-1740);
                        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        slide.setPower(0.2); // Small power to maintain position
                    }

                } else if (gamepad1.dpad_down) {
                /*    if (button.isPressed() && !isPositionSet) {
                        // Button just pressed - start position hold
                        slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                        slide.setTargetPosition(0);
                        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                        slide.setPower(HOLDING_POWER);
                        isPositionSet = true;
                        positionHoldStartTime = System.currentTimeMillis();
                    }*/
                    if (!button.isPressed() && !isPositionSet) {
                        // Moving in, but button not pressed yet
                        slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        slide.setPower(1);
                    }
                } else if (!isPositionSet && !sequenceStarted) {
                    // Stop if not holding position
                    slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    slide.setPower(0);
                }


                if (button.isPressed() && !isPositionSet && !sequenceStarted) {
                    // Button just pressed - start position hold
                    slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    slide.setTargetPosition(0);
                    slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    slide.setPower(HOLDING_POWER);
                    isPositionSet = true;
                    positionHoldStartTime = System.currentTimeMillis();
                    // unload_on_button_lock = true;
                }
                // slide
// Check if we should stop holding position
                if (isPositionSet && (System.currentTimeMillis() - positionHoldStartTime > HOLD_DURATION)) {
                    slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    slide.setPower(0);
                    isPositionSet = false;
                }


                if (gamepad1.x && !sequenceStarted) {  // Only trigger once when x is first pressed
                    sequenceStarted = true;  // Start the sequence
                    slide_set = false;   // Reset position flag
                    tilt.setPosition(0.01);  // Flip back immediately
                }

                if (sequenceStarted) {
                    if (!slide_set) {
                        // Move slide until button is pressed
                        if (!button.isPressed()) {
                            slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                            slide.setPower(1);
                        }
                        if (button.isPressed()) {
                            unroll_start_time = System.currentTimeMillis();  // Start timer for roller
                            unload_on_button_lock = true;

                            slide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                            slide.setTargetPosition(0);
                            slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                            slide.setPower(HOLDING_POWER);
                            slide_set = true;
                            positionHoldStartTime = System.currentTimeMillis();
                        }
                    }
                    // slide
// Check if we should stop holding position
                    if (slide_set && (System.currentTimeMillis() - positionHoldStartTime > HOLD_DURATION)) {
                        slide.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                        slide.setPower(0);
                    }
                }

                if (unload_on_button_lock) {
                    if ((System.currentTimeMillis() - unroll_start_time) > 3000) {
                        roller.setPower(0);  // Stop roller after 2 seconds
                        unload_on_button_lock = false;
                        sequenceStarted = false;  // Reset sequence
                    } else if ((System.currentTimeMillis() - unroll_start_time) > 1000 && tilt.getPosition() <= 0.02) {
                        roller.setPower(-255);  // Run roller only if tilt condition is met
                    }
                }


                telemetry.addData("Tilt Position", tilt.getPosition());
                telemetry.addData("Button Pressed", button.isPressed());
                telemetry.addData("Motor Position", slide.getCurrentPosition());
                telemetry.addData("Is Position Set", isPositionSet);
                telemetry.addData("Hold Time Remaining",
                        isPositionSet ? (HOLD_DURATION - (System.currentTimeMillis() - positionHoldStartTime)) : 0);
                telemetry.addData("Elevation Hold time remaining", elevation_locked ? ((5000 - (System.currentTimeMillis() - lock_start_time))) / 1000 : 0);


                // roller

                if (gamepad1.a) {
                    roller.setPower(255);  // Full power forward
                } else if (gamepad1.b) {
                    roller.setPower(-255); // Full power reverse
                } else if (!sequenceStarted) {
                    roller.setPower(0);    // Stop
                }

                // tilt
                if (gamepad1.dpad_left) {
                    tilt.setPosition(-0.6);


                } else if (gamepad1.dpad_right) {
                    tilt.setPosition(0.74);
                }


                // dump
                if (gamepad1.y) {
                    dump.setPosition(0.2);
                } else {
                    dump.setPosition(0.4);
                }


                FL.setPower(driveSpeed * (turn_FL_X + strafe_FL_X + strafe_FL_Y));
                FR.setPower(driveSpeed * (turn_FR_X + strafe_FR_X + strafe_FR_Y));
                BL.setPower(driveSpeed * (turn_BL_X + strafe_BL_X + strafe_BL_Y));
                BR.setPower(driveSpeed * (turn_BR_X + strafe_BR_X + strafe_BR_Y));


                telemetryAprilTag();
                sleep(20);

                telemetry.update();
            }
        }
    }


    public void initAprilTag() {
        // Create the AprilTag processor the easy way.
        aprilTag = AprilTagProcessor.easyCreateWithDefaults();

        // Create the vision portal using the builder
        if (USE_WEBCAM) {
            visionPortal = new VisionPortal.Builder()
                    .setCamera(hardwareMap.get(WebcamName.class, "webcam1"))
                    .addProcessor(aprilTag)
                    .enableLiveView(true)
                    .setAutoStartStreamOnBuild(true)
                    .setStreamFormat(VisionPortal.StreamFormat.MJPEG)
                    .build();
        } else {
            visionPortal = VisionPortal.easyCreateWithDefaults(
                    BuiltinCameraDirection.BACK, aprilTag);
        }
    }   // end method initAprilTag()

    private void telemetryAprilTag() {

        List<AprilTagDetection> currentDetections = aprilTag.getDetections();
        telemetry.addData("# AprilTags Detected", currentDetections.size());

        // Step through the list of detections and display info for each one.
        for (AprilTagDetection detection : currentDetections) {
            if (detection.metadata != null) {
                telemetry.addLine(String.format("\n==== (ID %d) %s", detection.id, detection.metadata.name));
                telemetry.addLine(String.format("XYZ %6.1f %6.1f %6.1f  (inch)", detection.ftcPose.x, detection.ftcPose.y, detection.ftcPose.z));
                telemetry.addLine(String.format("PRY %6.1f %6.1f %6.1f  (deg)", detection.ftcPose.pitch, detection.ftcPose.roll, detection.ftcPose.yaw));
                telemetry.addLine(String.format("RBE %6.1f %6.1f %6.1f  (inch, deg, deg)", detection.ftcPose.range, detection.ftcPose.bearing, detection.ftcPose.elevation));
            } else {
                telemetry.addLine(String.format("\n==== (ID %d) Unknown", detection.id));
                telemetry.addLine(String.format("Center %6.0f %6.0f   (pixels)", detection.center.x, detection.center.y));
            }
        }   // end for() loop

        // Add "key" information to telemetry
        telemetry.addLine("\nkey:\nXYZ = X (Right), Y (Forward), Z (Up) dist.");
        telemetry.addLine("PRY = Pitch, Roll & Yaw (XYZ Rotation)");
        telemetry.addLine("RBE = Range, Bearing & Elevation");

    }   // end method telemetryAprilTag()



} // end class
