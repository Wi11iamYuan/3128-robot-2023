package frc.team3128.subsystems.pivot;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismRoot2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.PIDSubsystem;
import static frc.team3128.Constants.PivotConstants.*;

import java.util.function.DoubleSupplier;

import org.littletonrobotics.junction.Logger;

import frc.team3128.Robot;
import frc.team3128.RobotContainer;
import frc.team3128.commands.CmdSimPivot;
import frc.team3128.Constants.PivotConstants;
import frc.team3128.Constants.TelescopeConstants;
import frc.team3128.Constants.ArmConstants.ArmPosition;
import frc.team3128.common.hardware.motorcontroller.NAR_CANSparkMax;
import frc.team3128.common.utility.NAR_Shuffleboard;
import frc.team3128.subsystems.Telescope;

import com.revrobotics.CANSparkMax.IdleMode;

public class Pivot extends PIDSubsystem {
    // kV * desired_velocity = voltage
    // desired_velo = voltage / kV

    private DoubleSupplier kF, kG, setpoint;

    private static Pivot instance;
    private NAR_CANSparkMax m_rotateMotor;
    public double offset;

    private PivotIO pivotIO;
    private PivotIOInputsAutoLogged pivotIOInputs = new PivotIOInputsAutoLogged();

    private SingleJointedArmSim m_singleJointedArmSim = new SingleJointedArmSim(
        DCMotor.getNEO(1), 
        GEAR_RATIO,
        jKgMetersSquared, // TODO: find this
        Units.inchesToMeters(ARM_LENGTH), 
        minAngleDegs,
        maxAngleDegs, 
        true
    );
    private Mechanism2d m_mech2d;
    private MechanismRoot2d m_mech2dRoot;
    private MechanismLigament2d m_pivotMech2d;


    public Pivot() {
        super(new PIDController(kP, kI, kD));

        pivotIO = new PivotIO();
        // getController().enableContinuousInput(-180, 180);

        configMotors();
        getController().setTolerance(PIVOT_TOLERANCE);

        setSetpoint(getMeasurement());
        
        if(Robot.isSimulation()) {
            m_mech2d = new Mechanism2d(100, 100);
            m_mech2dRoot = m_mech2d.getRoot("Pivot Root", 50, 50);
            m_pivotMech2d = m_mech2dRoot.append(
                new MechanismLigament2d("Pivot", 50, -90));
            SmartDashboard.putData("Pivot Sim", m_mech2d);
            // m_pivotMech2d.setLength(50);
            
        }
    }

    public void simulationPeriodic() {
        m_singleJointedArmSim.setInputVoltage(
            m_rotateMotor.getSimVoltage()
            
        );
        m_singleJointedArmSim.update(0.02);
        m_pivotMech2d.setAngle(m_singleJointedArmSim.getAngleRads() - 90);

    }

    public static synchronized Pivot getInstance(){
        if (instance == null) {
            instance = new Pivot();
        }
        return instance;
    }

    @Override
    public void periodic(){
        pivotIO.updateInputs(pivotIOInputs);
        Logger.getInstance().processInputs("Pivot", pivotIOInputs);
        super.periodic();
    }

    private void configMotors() {
        m_rotateMotor = new NAR_CANSparkMax(PIVOT_MOTOR_ID);
        m_rotateMotor.setSmartCurrentLimit(PIVOT_CURRENT_LIMIT);
        m_rotateMotor.setInverted(false);
        m_rotateMotor.enableVoltageCompensation(12.0);
        m_rotateMotor.setIdleMode(IdleMode.kBrake);
        resetPivot();
    }

    public void setPower(double power) {
        disable();
        m_rotateMotor.set(power);
    }

    public void resetPivot() {
        m_rotateMotor.setSelectedSensorPosition(0);
    }

    public void stopPivot() {
        setPower(0);
    }

    public double getAngle(){
        return Robot.isReal() ? m_rotateMotor.getSelectedSensorPosition() * 360 / GEAR_RATIO : m_singleJointedArmSim.getAngleRads();
    }

    @Override
    public double getMeasurement() { // returns degrees
        return m_rotateMotor.getSelectedSensorPosition() * 360 / GEAR_RATIO;
    }

    public void startPID(double anglePos) {
        anglePos = RobotContainer.DEBUG.getAsBoolean() ? setpoint.getAsDouble() : anglePos;
        anglePos = MathUtil.clamp(anglePos,minAngleDegs,maxAngleDegs);
        enable();
        setSetpoint(anglePos);
    }

    public void startPID(ArmPosition position) {
        startPID(position.pivotAngle);
    }

    @Override
    protected void useOutput(double output, double setpoint) {
        // SmartDashboard.putNumber("Test", 1);
        double fG = kG.getAsDouble() * Math.sin(Units.degreesToRadians(setpoint)); 
        double teleDist = (Robot.isReal() ? Telescope.getInstance().getDist() : ARM_LENGTH);

        fG *= 1.0/14.25 * (teleDist - TelescopeConstants.MIN_DIST) + 1;
        //fG *= MathUtil.clamp(((teleDist-11.5) / (TelescopeConstants.MAX_DIST - TelescopeConstants.MIN_DIST)),0,1); 

        double voltageOutput = output + fG;

        //if (Math.abs(setpoint - getAngle()) > kF.getAsDouble()) voltageOutput = Math.copySign(12, voltageOutput);
        
        m_rotateMotor.set(MathUtil.clamp(voltageOutput / 12.0, -1, 1));
        Logger.getInstance().recordOutput("Pivot-output", MathUtil.clamp(voltageOutput / 12.0, -1, 1));
        Logger.getInstance().recordOutput("Pivot-setpoint", setpoint);
        m_rotateMotor.setSimVoltage(voltageOutput);
    }

    public boolean atSetpoint() {
        return getController().atSetpoint();
    }

    public void initShuffleboard() {
        // NAR_Shuffleboard.addData("pivot","encoder angle", ()->getAngle(),0,3);
        NAR_Shuffleboard.addData("pivot","pivot angle", ()->getMeasurement(),0,0);
        NAR_Shuffleboard.addData("pivot", "pivot setpoint", ()->getSetpoint(), 0, 1);
        kG = NAR_Shuffleboard.debug("pivot","kG", PivotConstants.kG, 0,4);
        kF = NAR_Shuffleboard.debug("pivot","kF", PivotConstants.kF, 0,2);
        setpoint = NAR_Shuffleboard.debug("pivot", "setpoint", 0, 1,2);
        NAR_Shuffleboard.addComplex("pivot", "Pivot-PID",m_controller, 2, 0);
        NAR_Shuffleboard.addData("pivot", "atSetpoint", ()->getController().atSetpoint(), 3, 0);
        NAR_Shuffleboard.addData("pivot", "isEnabled", ()->isEnabled(), 4, 0);
    }
}
