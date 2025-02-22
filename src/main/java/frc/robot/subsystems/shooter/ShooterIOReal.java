// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems.shooter;

import edu.wpi.first.units.Units.*;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.Volts;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.controls.MotionMagicVelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.Current;
import edu.wpi.first.units.measure.Voltage;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import frc.robot.Constants;

/** Add your docs here. */
public class ShooterIOReal implements ShooterIO {

    private TalonFX m_leftFlywheel = new TalonFX(Constants.LEFT_FLYWHEEL);
    private TalonFX m_rightFlywheel = new TalonFX(Constants.RIGHT_FLYWHEEL);

    private final MotionMagicVelocityVoltage m_request = new MotionMagicVelocityVoltage(0);
    private Alert shooterDisconnectAlert;

    private final StatusSignal<Angle> position;
    private final StatusSignal<AngularVelocity> velocity;
    private final StatusSignal<Voltage> voltage;
    private final StatusSignal<Current> current;


    public ShooterIOReal() {
        //constructor go brrrrrrr
        var leftConfig = new TalonFXConfiguration();
        //set inverted here
        leftConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
        leftConfig.CurrentLimits.SupplyCurrentLimit = Constants.SHOOTER_SUPPLY_CURRENT_LIMIT;

        leftConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        leftConfig.CurrentLimits.StatorCurrentLimit = Constants.SHOOTER_STATOR_CURRENT_LIMIT;

        leftConfig.MotorOutput.NeutralMode = NeutralModeValue.Coast;

        // we don't need this anymore: leftConfig.MotorOutput.PeakReverseDutyCycle = 0;

        var leftSlot0Config = leftConfig.Slot0;
        leftSlot0Config.kS = 0.5; // Add 0.25 V output to overcome static friction
        leftSlot0Config.kV = 0.15; // A velocity target of 1 rps results in 0.12 V output
        leftSlot0Config.kA = 0.02; // An acceleration of 1 rps/s requires 0.01 V output
        leftSlot0Config.kP = 0.4; // An error of 1 rps results in 0.11 V output
        leftSlot0Config.kI = 0.0; // no output for integrated error
        leftSlot0Config.kD = 0.0; // no output for error derivative

        var leftMotionMagicConfig = leftConfig.MotionMagic;
        leftMotionMagicConfig.MotionMagicAcceleration = 125; // Target acceleration of 400 rps/s (0.25 seconds to max)
        leftMotionMagicConfig.MotionMagicJerk = 4000; // Target jerk of 4000 rps/s/s (0.1 seconds)

        m_leftFlywheel.getConfigurator().apply(leftConfig);

        var rightConfig = new TalonFXConfiguration();

        rightConfig.CurrentLimits.SupplyCurrentLimitEnable = true;
        rightConfig.CurrentLimits.SupplyCurrentLimit = Constants.SHOOTER_SUPPLY_CURRENT_LIMIT;

        rightConfig.CurrentLimits.StatorCurrentLimitEnable = true;
        rightConfig.CurrentLimits.StatorCurrentLimit = Constants.SHOOTER_STATOR_CURRENT_LIMIT;

        m_rightFlywheel.getConfigurator().apply(rightConfig);

        m_rightFlywheel.setControl(new Follower(Constants.LEFT_FLYWHEEL, true));

        shooterDisconnectAlert = new Alert("Shooter motor is not present on CAN", AlertType.kError);
        
        velocity = m_leftFlywheel.getVelocity();
        current = m_leftFlywheel.getSupplyCurrent();
        voltage = m_leftFlywheel.getMotorVoltage();
        position = m_leftFlywheel.getPosition();

        BaseStatusSignal.setUpdateFrequencyForAll(
                100.0, velocity);
        BaseStatusSignal.setUpdateFrequencyForAll(50,
                current,
                voltage,
                position);
        m_leftFlywheel.optimizeBusUtilization();
    }

    public void updateInputs(ShooterIOInputs inputs) {
        BaseStatusSignal.refreshAll(
                velocity,
                current,
                voltage,
                position);

        inputs.shooterVelocityMPS = RotationsPerSecond.of((m_leftFlywheel.getVelocity().getValueAsDouble())*Constants.FLYWHEEL_CIRCUMFERENCE);
        inputs.shooterCurrentAmps = Amps.of(m_leftFlywheel.getSupplyCurrent().getValueAsDouble());
        inputs.shooterVoltage = Volts.of(m_leftFlywheel.getMotorVoltage().getValueAsDouble());
        inputs.motorPosition = Rotations.of(m_leftFlywheel.getPosition().getValueAsDouble());
        inputs.motorSetpoint = RotationsPerSecond.of(m_request.Velocity * Constants.FLYWHEEL_CIRCUMFERENCE);

        shooterDisconnectAlert.set(!position.getStatus().isOK());
    }

    public void setVoltage(double volts){
        m_leftFlywheel.setVoltage(volts);
    }

    public void setFlywheelSpeed(double velocity){
        // m_leftFlywheel.setVoltage(velocity);

        // VELOCITY IN MPS
        velocity = velocity/Constants.FLYWHEEL_CIRCUMFERENCE;
        m_leftFlywheel.setControl(m_request.withVelocity(velocity));
    }

    public void stopFlywheel(){
        m_leftFlywheel.set(0);
    }
}
