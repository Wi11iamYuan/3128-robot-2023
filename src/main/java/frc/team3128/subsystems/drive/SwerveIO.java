package frc.team3128.subsystems.drive;

import org.littletonrobotics.junction.AutoLog;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;


public class SwerveIO {
    @AutoLog
    public static class SwerveIOInputs {
        // public Pose2d estimatedPose = new Pose2d();
        public double[] moduleSpeeds = new double[4];
        public double[] moduleAngles = new double[4];
        public double[] modulePositions = new double[4];
        public double yaw = 0.0;
        public double pitch = 0.0;
        public double roll = 0.0;
    }

    public void updateInputs(SwerveIOInputs inputs) {
        SwerveModuleState[] states = Swerve.getInstance().getStates();
        SwerveModulePosition[] positions = Swerve.getInstance().getPositions();
        for(int i = 0; i < 4; i++){
            inputs.moduleSpeeds[i] = states[i].speedMetersPerSecond;
            inputs.moduleAngles[i] = states[i].angle.getDegrees();
            inputs.modulePositions[i] = positions[i].distanceMeters;
        }
        // inputs.estimatedPose = Swerve.getInstance().getPose();
        inputs.yaw = Swerve.getInstance().getYaw();
        inputs.pitch = Swerve.getInstance().getPitch();
        inputs.roll = Swerve.getInstance().getRoll();
    }
}
