/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package edu.wpi.first.wpilibj.defaultCode;

import edu.wpi.first.wpilibj.AnalogChannel;
import edu.wpi.first.wpilibj.AnalogModule;
import edu.wpi.first.wpilibj.CANJaguar;
import edu.wpi.first.wpilibj.Dashboard;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DigitalOutput;
import edu.wpi.first.wpilibj.DigitalModule;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.Watchdog;

public class DefaultRobotA1 extends IterativeRobot {
    // Declare variable for the robot drive system
    RobotDrive m_robotDrive;		// robot will use PWM 1-4 for drive motors
    //MotorControl m_robotDrive;
    DriverStation ds;

    CANJaguar frontLeftJag;
    CANJaguar frontRightJag;
    CANJaguar rearLeftJag;
    CANJaguar rearRightJag;

    CANJaguar boomRightJag;
    CANJaguar boomLeftJag;
    CANJaguar stickRightJag;
    CANJaguar stickLeftJag;

    int currentPreset = -1;
    int presetStage = 0;

    DigitalOutput deployServo;

    DigitalInput leftLineSensor;
    DigitalInput middleLineSensor;
    DigitalInput rightLineSensor;
    int[] lastThreeLines = new int[3];
    int lineLoops = 0;
    boolean hitY = false;
	boolean hasStopped = false;
	int ignoreLines = 0;

    int m_dsPacketsReceivedInCurrentSecond;	// keep track of the ds packets received in the current second

    // Declare variables for the two joysticks being used
    Joystick m_rightStick;			// joystick 1 (arcade stick or right tank stick)

    static final int NUM_BUTTONS = 10;
	boolean[] m_buttons = new boolean[NUM_BUTTONS];

    // Declare variables for each of the eight solenoid outputs
    static final int NUM_SOLENOIDS = 2;
    Solenoid[] m_solenoids = new Solenoid[NUM_SOLENOIDS];

    // drive mode selection
    static final int UNINITIALIZED_DRIVE = 0;
    static final int ARCADE_DRIVE = 1;
    static final int TANK_DRIVE = 2;
    int m_driveMode;

    boolean slow_drive = false;

    // Local variables to count the number of periodic loops performed
    int m_autoPeriodicLoops;
    int m_disabledPeriodicLoops;
    int m_telePeriodicLoops;

    /**
     * Constructor for this "BuiltinDefaultCode" Class.
     *
     * The constructor creates all of the objects used for the different inputs and outputs of
     * the robot.  Essentially, the constructor defines the input/output mapping for the robot,
     * providing named objects for each of the robot interfaces.
     */
    public DefaultRobotA1() {
        System.out.println("BuiltinDefaultCode Constructor Started\n");

        ds = DriverStation.getInstance();

        // Create a robot using standard right/left robot drive on PWMS 1, 2, 3, and #4

        try {
            frontLeftJag = new CANJaguar(4);
            frontRightJag = new CANJaguar(5);
            rearLeftJag = new CANJaguar(3);
            rearRightJag = new CANJaguar(2);
        } catch (Exception e) {}

        m_robotDrive = new RobotDrive(frontLeftJag, rearLeftJag, frontRightJag, rearRightJag);
        m_robotDrive.setInvertedMotor(RobotDrive.MotorType.kFrontLeft, true);
        m_robotDrive.setInvertedMotor(RobotDrive.MotorType.kRearLeft, true);
        m_robotDrive.setInvertedMotor(RobotDrive.MotorType.kFrontRight, true);
        m_robotDrive.setInvertedMotor(RobotDrive.MotorType.kRearRight, true);

        /*
        try {
            frontLeftJag= new CANJaguar(4, CANJaguar.ControlMode.kSpeed);
            frontRightJag = new CANJaguar(5, CANJaguar.ControlMode.kSpeed);
            rearLeftJag = new CANJaguar(3, CANJaguar.ControlMode.kSpeed);
            rearRightJag = new CANJaguar(2, CANJaguar.ControlMode.kSpeed);

            m_robotDrive = new MotorControl(frontRightJag, rearRightJag, frontLeftJag, rearLeftJag);
        } catch (Exception e) {}

        try {
            boomRightJag = new CANJaguar(6, CANJaguar.ControlMode.kPosition);
            boomLeftJag = new CANJaguar(7, CANJaguar.ControlMode.kPosition);
            stickRightJag = new CANJaguar(8, CANJaguar.ControlMode.kPosition);
            stickLeftJag = new CANJaguar(9, CANJaguar.ControlMode.kPosition);
            boomRightJag.setPositionReference(CANJaguar.PositionReference.kPotentiometer);
            boomLeftJag.setPositionReference(CANJaguar.PositionReference.kPotentiometer);
            stickRightJag.setPositionReference(CANJaguar.PositionReference.kPotentiometer);
            stickLeftJag.setPositionReference(CANJaguar.PositionReference.kPotentiometer);
        } catch (Exception e) {}*/

        deployServo = new DigitalOutput(1);

        leftLineSensor = new DigitalInput(14);
        middleLineSensor = new DigitalInput(13);
        rightLineSensor = new DigitalInput(12);

        m_dsPacketsReceivedInCurrentSecond = 0;

        // Define joysticks being used at USB port #1 and USB port #2 on the Drivers Station
        m_rightStick = new Joystick(1);

        // Iterate over all the solenoids on the robot, constructing each in turn
        int solenoidNum = 1;						// start counting solenoids at solenoid 1
        for (solenoidNum = 0; solenoidNum < NUM_SOLENOIDS; solenoidNum++) {
            m_solenoids[solenoidNum] = new Solenoid(solenoidNum + 1);
        }

        // Set drive mode to uninitialized
        m_driveMode = ARCADE_DRIVE;

        // Initialize counters to record the number of loops completed in autonomous and teleop modes
        m_autoPeriodicLoops = 0;
        m_disabledPeriodicLoops = 0;
        m_telePeriodicLoops = 0;

        System.out.println("BuiltinDefaultCode Constructor Completed\n");
    }

    /********************************** Init Routines *************************************/

    public void robotInit() {
        // Actions which would be performed once (and only once) upon initialization of the
        // robot would be put here.

        System.out.println("RobotInit() completed.\n");
    }

    public void disabledInit() {
        m_disabledPeriodicLoops = 0;			// Reset the loop counter for disabled mode
        startSec = (int)(Timer.getUsClock() / 1000000.0);
        printSec = startSec + 1;
    }

    public void autonomousInit() {
        m_autoPeriodicLoops = 0;
    }

    public void teleopInit() {
        m_telePeriodicLoops = 0;                    // Reset the loop counter for teleop mode
        m_dsPacketsReceivedInCurrentSecond = 0;     // Reset the number of dsPackets in current second
        m_driveMode = TANK_DRIVE;                   // Set drive mode to uninitialized
    }

    /********************************** Periodic Routines *************************************/
    static int printSec;
    static int startSec;

    public void disabledPeriodic() {
        updateDashboard();

        lastThreeLines[lineLoops%3] = 0;
        lastThreeLines[lineLoops%3] += (leftLineSensor.get()) ? 0 : 100;
        lastThreeLines[lineLoops%3] += (middleLineSensor.get()) ? 0 : 10;
        lastThreeLines[lineLoops%3] += (rightLineSensor.get()) ? 0 : 1;
        lineLoops++;

        // feed the user watchdog at every period when disabled
        Watchdog.getInstance().feed();

        // increment the number of disabled periodic loops completed
        m_disabledPeriodicLoops++;
		lineLoops = 0;
		hitY = false;
		hasStopped = false;
		ignoreLines = 0;
    }

    public void autonomousPeriodic() {
        updateDashboard();
        // feed the user watchdog at every period when in autonomous
        Watchdog.getInstance().feed();

        m_autoPeriodicLoops++;

        if (!hasStopped) {//m_autoPeriodicLoops % (GetLoopsPerSec()/1.0) == 0) {
			boolean straightLine = ds.getDigitalIn(1);
			boolean goLeft = ds.getDigitalIn(2) && !straightLine;

			int lineReading = 0;
			lineReading += (leftLineSensor.get()) ? 0 : 100;
			lineReading += (middleLineSensor.get()) ? 0 : 10;
			lineReading += (rightLineSensor.get()) ? 0 : 1;
			System.out.print("" + m_autoPeriodicLoops + " | " + lineReading + " | ");

			double rightMotors = 0;
			double leftMotors = 0;

			switch (lineReading) {
				case   0:
					if(!straightLine && !hitY){
						if(!hitY){
							if (goLeft) {
								leftMotors = 0.7;
								rightMotors = -0.7;
								lineReading = 100;
							}
							else {
								leftMotors = -0.7;
								rightMotors = 0.7;
								lineReading = 1;
							}
						}
						/*else{
							if(goLeft) {
								rightMotors = -0.6;//*(lastThreeLines[0]%10 + lastThreeLines[1]%10 + lastThreeLines[2]%10)/3.0;
								leftMotors = 0.9;//*((lastThreeLines[0]/100)%10 + (lastThreeLines[1]/100)%10 + (lastThreeLines[2]/100)%10)/3.0;
							}
							else {
								rightMotors = 0.9;//*(lastThreeLines[0]%10 + lastThreeLines[1]%10 + lastThreeLines[2]%10)/3.0;
								leftMotors = -0.6;//*((lastThreeLines[0]/100)%10 + (lastThreeLines[1]/100)%10 + (lastThreeLines[2]/100)%10)/3.0;
							}
						}*/
					}
					else{
						rightMotors = 1.2 * (lastThreeLines[0] % 10 + lastThreeLines[1] % 10 + lastThreeLines[2] % 10) / 3.0;
						leftMotors = 1.2*((lastThreeLines[0]/100)%10 + (lastThreeLines[1]/100)%10 + (lastThreeLines[2]/100)%10)/3.0;
					}
					break;
				case   1: leftMotors = 0.55; rightMotors = 0.7; break;
				case  11: leftMotors = 0.6; rightMotors = 0.7; break;
				case  10: leftMotors = 0.8; rightMotors = 0.8; break;
				case 110: leftMotors = 0.7; rightMotors = 0.6; break;
				case 100: leftMotors = 0.7; rightMotors = 0.55; break;
				case 111:
					if (straightLine || hitY) {
						hasStopped = true;
						System.out.print("STOPPED! | ");
					} else {
						if (goLeft) {
							leftMotors = 0.7;
							rightMotors = -0.7;
							lineReading = 100;
						} else {
							leftMotors = -0.7;
							rightMotors = 0.7;
							lineReading = 1;
						}
						hitY = true;
						System.out.print("HIT! | ");
					}
					break;
				case 101:
					if (!straightLine) {
						if (goLeft) {
							leftMotors = 0.7;
							rightMotors = -0.7;
							lineReading = 100;
						} else {
							leftMotors = -0.7;
							rightMotors = 0.7;
							lineReading = 1;
						}
						hitY = true;
						System.out.print("HIT! | ");
					}
					break;
				default: break;
			}

			m_robotDrive.tankDrive(-leftMotors, -rightMotors);
			System.out.println("Left: " + leftMotors + ", Right: " + rightMotors);

			if(lineReading > 0) lastThreeLines[lineLoops%3] = lineReading;
			lineLoops++;
        }
		else if(ignoreLines > 40){
			int lineReading = 0;
			lineReading += (leftLineSensor.get()) ? 0 : 100;
			lineReading += (middleLineSensor.get()) ? 0 : 10;
			lineReading += (rightLineSensor.get()) ? 0 : 1;
			System.out.print("" + lineReading + " | ");

			double rightMotors = 0;
			double leftMotors = 0;

			switch (lineReading) {
				case   0:
					leftMotors = 1*(lastThreeLines[0]%10 + lastThreeLines[1]%10 + lastThreeLines[2]%10)/3.0;
					rightMotors = 1*((lastThreeLines[0]/100)%10 + (lastThreeLines[1]/100)%10 + (lastThreeLines[2]/100)%10)/3.0;
					break;
				case   1: leftMotors = 0.4; rightMotors = 0.8; break;
				case  10: leftMotors = 0.9; rightMotors = 0.9; break;
				case  11: leftMotors = 0.7; rightMotors = 0.9; break;
				case 100: leftMotors = 0.8; rightMotors = 0.4; break;
				case 110: leftMotors = 0.9; rightMotors = 0.7; break;
				case 111: leftMotors = 0.8; rightMotors = 0.8; break;
				case 101: leftMotors = 0.8; rightMotors = 0.8; break;
				default: break;
			}

			m_robotDrive.tankDrive(-leftMotors, -rightMotors);
			System.out.println("Left: " + leftMotors + ", Right: " + rightMotors);

			lastThreeLines[lineLoops%3] = lineReading;
			lineLoops++;
		}
		else {
			ignoreLines++;
			m_robotDrive.tankDrive(0.8, -0.8);
			System.out.println("Ignoring line");
		}
    }

    public void teleopPeriodic() {
		lineLoops = 0;
		hitY = false;
		hasStopped = false;
		ignoreLines = 0;
        updateDashboard();

        // feed the user watchdog at every period when in autonomous
        Watchdog.getInstance().feed();

        // increment the number of teleop periodic loops completed
        m_telePeriodicLoops++;

        // increment DS packets received
        m_dsPacketsReceivedInCurrentSecond++;

		for (int buttonDex = 0; buttonDex < NUM_BUTTONS; buttonDex++) {
			m_buttons[buttonDex] = m_rightStick.getRawButton(buttonDex);
		}

        // drive with arcade style
		/*if(m_buttons[7])
			m_robotDrive.tankDrive(-m_rightStick.getRawAxis(3), -m_rightStick.getRawAxis(2));
		else
			m_robotDrive.tankDrive(m_rightStick.getRawAxis(2), m_rightStick.getRawAxis(3));*/
		m_robotDrive.arcadeDrive(((m_buttons[7])?-1:1)*m_rightStick.getRawAxis(2), m_rightStick.getRawAxis(4)/3*2, false);
        //m_robotDrive.pidWrite(-m_rightStick.getRawAxis(1));

        if (currentPreset == -1) {
            int presetButton = 0;
            for (presetButton = 0; presetButton < NUM_BUTTONS; presetButton++) {
                if (m_buttons[presetButton])
                    break;
            }
            currentPreset = presetButton;
        } else {
            // Nothing for now
        }
    }

    int GetLoopsPerSec() {
        return 20;
    }

    void updateDashboard() {
        Dashboard lowDashData = ds.getDashboardPackerLow();
        lowDashData.addCluster();
        {
            lowDashData.addCluster();
            {     //analog modules
                lowDashData.addCluster();
                {
                    for (int i = 1; i <= 8; i++) {
                        lowDashData.addFloat((float) AnalogModule.getInstance(1).getAverageVoltage(i));
                    }
                }
                lowDashData.finalizeCluster();
                lowDashData.addCluster();
                {
                    for (int i = 1; i <= 8; i++) {
                        lowDashData.addFloat((float) AnalogModule.getInstance(2).getAverageVoltage(i));
                    }
                }
                lowDashData.finalizeCluster();
            }
            lowDashData.finalizeCluster();

            lowDashData.addCluster();
            { //digital modules
                lowDashData.addCluster();
                {
                    lowDashData.addCluster();
                    {
                        int module = 4;
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addShort(DigitalModule.getInstance(module).getAllDIO());
                        lowDashData.addShort(DigitalModule.getInstance(module).getDIODirection());
                        lowDashData.addCluster();
                        {
                            for (int i = 1; i <= 10; i++) {
                                lowDashData.addByte((byte) DigitalModule.getInstance(module).getPWM(i));
                            }
                        }
                        lowDashData.finalizeCluster();
                    }
                    lowDashData.finalizeCluster();
                    lowDashData.addCluster();
                    {
                        int module = 6;
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addByte(DigitalModule.getInstance(module).getRelayForward());
                        lowDashData.addShort(DigitalModule.getInstance(module).getAllDIO());
                        lowDashData.addShort(DigitalModule.getInstance(module).getDIODirection());
                        lowDashData.addCluster();
                        {
                            for (int i = 1; i <= 10; i++) {
                                lowDashData.addByte((byte) DigitalModule.getInstance(module).getPWM(i));
                            }
                        }
                        lowDashData.finalizeCluster();
                    }
                    lowDashData.finalizeCluster();
                }
                lowDashData.finalizeCluster();

            }
            lowDashData.finalizeCluster();
        }
        lowDashData.finalizeCluster();

        lowDashData.addByte(m_solenoids[0].getAll());

        lowDashData.commit();
    }
}