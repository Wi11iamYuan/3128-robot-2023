package frc.team3128.commands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.team3128.subsystems.Manipulator;

public class CmdManipulatorIntake extends CommandBase{
    
    private Manipulator manipulator;

    public CmdManipulatorIntake(){
        manipulator = Manipulator.getInstance();
        addRequirements(manipulator);
    }

    @Override
    public void initialize() {
        manipulator.openClaw();
        manipulator.enableRollersForward();
    }
    
    // public boolean isFinished(){
    //     return manipulator.hasObjectPresent();
    // }
}
