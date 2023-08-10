package frc.team3128.subsystems.pivot;

import org.littletonrobotics.junction.AutoLog;

public class PivotIO {
    @AutoLog
    public static class PivotIOInputs{
        public double measurement = 0.0;
    }

    public void updateInputs(PivotIOInputs inputs){
        inputs.measurement = Pivot.getInstance().getMeasurement();
    }
}
