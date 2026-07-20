<!-- [![Build](https://github.com/cLarson-asbHum/ftc-summer-25/actions/workflows/gradle-build.yaml/badge.svg?branch=sim-updater)](https://github.com/cLarson-asbHum/ftc-summer-25/actions/workflows/gradle-build.yaml) -->
[![Tests](https://github.com/cLarson-asbHum/hardware-faker/actions/workflows/gradle-test.yaml/badge.svg?branch=hardware-map)](https://github.com/cLarson-asbHum/ftc-summer-25/actions/workflows/gradle-test.yaml)

<!--
## Table of Contents

TODO: Table of contents??
-->

# HardwareFaker

This project contains several test fakes of hardware devices common to FTC 
projects. Currently implemented test doubles include the following:

 * `ColorRangeSensor`
 * `CRServoImplEx`
 * `DcMotorControllerEx`
 * `DcMotorImplEx`
 * `DigitalChannelController`
 * `DigitalChannelImpl`
 * `DistanceSensor`
 * `LED`
 * `LynxModule`
 * `LynxModule` (partial, see "[Known Issues](#known-issues)")
 * `LynxUsbDeviceImpl`
 * `ServoControllerEx`
 * `ServoImplEx`

Along with these, the following classes are planned to be implemented:

 * `AnalogInput`
 * `Gamepad`
 * `HardwareMap`
 * `IMU`
 * `Telemetry`
 * `TouchSensor`

The goal of these fakes is to ultimately be able to test and entire opmode 
completely offline (separate from a robot) and so have faster, more reliable 
development.

## Intallation

1. Copy the HardwareFaker directory into your project, and add the following 
   line to the top-level `settings.gradle`:

```gradle
include ":HardwareFaker" // Or whatever you rename the directory to.
```

2. Include the following code in your project's `build.gradle` (most commonly in 
   the `:TeamCode` subproject):

```gradle
dependencies {
    // ... other statements

    implementation project(':HardwareFaker') // Or whatever you rename the directory to.

    // ...
}
```

3. Import the desired test double. All fakes are located in the 
   `clarson.ftc.faker` package and have the class name formula of 
   `${HardwareDevice}Fake`. For example, to import the test double for 
   `DcMotorEx`, place the following import statement at the beginning of your 
   code:

```java
import clarson.ftc.faker.DcMotorExFake;
```

4. Construct the fake. `DcMotorExFake` is constructed with an RPM and number of 
   ticks per revolution, in that order. Specifics for other constructors vary.

5. Use the fake wherever you would use the ordinary object! All fakes implement 
   the corresponding interfaces; `DcMotorExFake` implements the `DcMotorEx` 
   interface from `com.qualcomm.robotcore.hardware`, and so the following code 
   is completely valid:

```java
DcMotorImplEx motor = new DcMotorImplExFake(312, 576.6);
motor.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
motor.getMode(); // Returns DcMotorEx.RunMode.RUN_USING_ENCODER

motor.setPower(0.33); // The motor's current position will begin to move
motor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER); // Resets getCurrentPosition()
```

**NOTE:** If you wanted the test double of `DcMotor`, you would simply use 
the `DcMotorImplExFake` constructor as normal but use the field/variable as 
`DcMotor`. In other words, line 1 in the previous example becomes

```java
DcMotor motor = new DcMotorImplExFake(312, 576.6);
// Previously was "DcMotorEx motor = new DcMotorImplExFake(312, 576.6)" 
```

This also applies for other interfaces, such as `Servo` and `CRServo`; use 
the fake with "Ex" in its name. For `Servo`, use `ServoImplExFake`, and with 
`CRServo` use `CRServoImplExFake`.  

## Using `ModularUpdater`

In real opmodes with real hardware, methods that send commands to the Lynx board
(a part of Control and Expansion Hubs) take time, upwards of 2 milliseconds. As 
a result, such hardware calls are responsible for both latency but also- and 
more importantly- the movement of actuators and updating of sensors after each 
hardware call. This effect can be simulated by manually calling `update()` on
fakes implementing the `Updateable` interface, or by registering such 
`Updateable`s with a `ModularUpdater` object.

`Updateable`s can be registered using the `ModularUpdater.register` method.

<!-- TODO: make this include a part about how Hardware maps auto register! -->

All `ModularUpdater`-related classes are in the `clarson.ftc.faker.updater`
package. This includes `ModularUpdater` and other classes, listed below:

| Symbol Name        | Synopsis                                                   |
| ------------------ | ---------------------------------------------------------- |
| `ModularUpdater`   | Concrete subclass of the `Updater` interface               |
| `Rotateable`       | Interface for fakes able to have external rotation applied |
| `SimulateDelay`    | Annotates a method as causing automatic updates            |
| `Updateable`       | Interface for fakes able to be registered with `Updater`s  |
| `Updater`          | Interface for ModularUpdater                               |
| `UpdatesWhen`      | Argument of `SimulateDelay`                                |
| `TwoWayUpdateable` | An `Updateable` that can remember registered `Updaters`    |


### Automatic `update()` Calls

Automatic updating is done whenever a registered `Updateable` calls a method 
whose real life counterpart would send a command to the Lynx board. Such methods 
include `setPower()`, `getMode()`, `setPositon()`, `getDistance()`, etc. This 
calls the `update()` method on all `Updateable`s registered on the same Updater. 

`@SimulateDelay` is used to document a method as causing an automatic update to 
all `Updater`s it is registered in. The meaning of each enum constant is listed
below:

| `UpdatesWhen` Constant | Description                                                                                   |
| ---------------------- | --------------------------------------------------------------------------------------------- |
| `ALWAYS`               | Always automatically updates all reigstered `Updater`s.                                       |
| `CONDITIONAL`          | Depends on a conditon that is *not* related to the BulkData cache                             |
| `NEVER`                | Never causes an automatic update                                                              |
| `ON_BULK_READ`         | Automatically updates whenever a reigstered `ModularUpdater` has an outdated LynxModule cache |

Although methods without this annotation can be assumed to never cause an 
automatic update, some methods may be explicitly annotated with 
`@SimulateDelay(UpdatesWhen.NEVER)`. 

**NOTE:** `@SimulateDelay` annotation is only retained in source, and cannot be
used at runtime to verify a method causes automatic updates.

Each automatic update has a delay length of 2.5 milliseconds unless the source 
is an I2C command, in which the delay is 7.5 ms. Manual updates must specify 
delay length in seconds using the `update()` method's parameter `deltaSec`.

**NOTE:** "Delay length" refers to the length of time *simulated*, not actually 
elapsing. In other words, `update()` does ***not*** block the calling thread for
2.5 milliseconds; instead, the real-life hardware the updated fake represents 
would block all threads for such time, causing all actuators to move and sensors
to update.

All classes capable of automatic updates implement the `TwoWayUpdateable` interface.

## Known Issues

 * `ServoControllerExFake.setServoType()` is provided for compatability and 
    does not affect how the servo interacts

 * `DcMotorControllerExFake.setMotorType()` only respects the `orientation` field
   on the given configuration, but changing this from CW to CCW is untested and 
   expected to cause unintended behavior. 

 * All actuators assume acceleration and friction are negligible. This is most
   notable with `DcMotorExFake.setVelocity()`, which instantly changes the
   velocity.

 * All PID(F) coefficient-setting methods in `DcMotorExFake` do nothing as neither
  `RUN_TO_POSITION` nor `RUN_USING_ENCODER` are implemented with PID(F)
   controllers.
 
 * Framing length in `PwmController.PwmRange` objects is ignored for all servo
   fakes, continuous or positional.

 * Not all `LynxMessage`s are supported to be transmitted to `LynxUsbDeviceImplFake`.
   For maintainers and advanced users, unsupported messages are sent `LynxNacks` with
   error code 254 (`COMMAND_ROUTING_ERROR`). Nothing is done except have an error be 
   recieved. The following codes are supported:

    * `LynxGetBulkInputDataCommand`,
    * `LynxGetMotorEncoderPositionCommand` (for bulk data retrieval),
    * `LynxIsMotorAtTargetCommand` (for bulk data retrieval),
    * `LynxKeepAliveCommand`,
    * `LynxQueryInterfaceCommand`.



<!--TODO: Create exmaples so that we can include the following:
## Examples 
TODO: create an example directory

**OpModes:**
TODO: create simple examples of OpMode features such as `InjectionHardwareMap`

**Basic Tests:** 
TODO: create examples showing the basic syntax and usage

**Automatic Updates:** 
TODO: create examples of how automatic updates work

**Wrappers:**
TODO: Create examples of using the data wrapper classes and controllers

**Sensors:**
TODO: Create examples of using sensor and setting the data
 -->