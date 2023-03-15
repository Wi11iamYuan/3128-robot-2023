package frc.team3128.commands;

import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.team3128.subsystems.Intake;
import frc.team3128.subsystems.Manipulator;
import frc.team3128.subsystems.Intake.IntakeState;

public class CmdIntake extends SequentialCommandGroup{

    public CmdIntake() {
        Intake intake = Intake.getInstance();
        
        addCommands(
            new InstantCommand(()-> intake.setForward(), intake),
            new CmdMoveIntake(IntakeState.DEPLOYED),
            new WaitCommand(0.1),
            new WaitUntilCommand(()->intake.hasObjectPresent()),
            new InstantCommand(()->intake.set(0.1)),
            new CmdMoveIntake(IntakeState.RETRACTED)
        );
    }
}
