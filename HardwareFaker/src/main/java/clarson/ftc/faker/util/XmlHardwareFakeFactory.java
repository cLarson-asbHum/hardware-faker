/*
 * XmlHardwareFakeFactory.java
 * 
 * Copyright 2026 Connor Larson
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package clarson.ftc.faker.util;

// TODO: Add more hardware!
// import clarson.ftc.faker.AnalogInputContollerFake;
// import clarson.ftc.faker.AnalogInputFake;
import clarson.ftc.faker.CRServoImplExFake;
import clarson.ftc.faker.DcMotorControllerExFake;
import clarson.ftc.faker.DcMotorImplExFake;
// import clarson.ftc.faker.DigitalChannelControllerFake;
// import clarson.ftc.faker.DigitalChannelFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.ServoControllerExFake;
import clarson.ftc.faker.ServoImplExFake;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.wrapper.ContinuousServoData;
import clarson.ftc.faker.wrapper.MotorData;
import clarson.ftc.faker.wrapper.PositionalServoData;

import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;

import static com.qualcomm.robotcore.hardware.PwmControl.PwmRange;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.Map.entry;    

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;

public class XmlHardwareFakeFactory {
    public static enum HardwareType {
        // TODO: Add more hardware types!
        ANALOG_INPUT,
        ANALOG_INPUT_CONTROLLER,
        COLOR_DISTANCE_SENSOR,
        CR_SERVO(new String[] {"port, rpm", "pwmMin", "pwmMax"}),
        DIGITAL_DEVICE,
        DIGITAL_DEVICE_CONTROLLER,
        DISTANCE_SENSOR,
        IMU,
        LED,
        LYNX_MODULE(new String[] {"port"}),
        LYNX_USB_DEVICE(new String[] {"parentModuleAddress", "serial"}),
        MOTOR(new String[] {"port, rpm", "ticksPerRev"}),
        MOTOR_CONTROLLER,
        SERVO(new String[] {"port, rpm", "turns", "pwmMin", "pwmMax"}),
        SERVO_CONTROLLER,
        TOUCH_SENSOR,
        UNKNOWN;

        public final String[] attributes;

        private HardwareType(String[] attributes) {
            this.attributes  = attributes;
        }

        private HardwareType() {
            this(new String[0]);
        }

        public String get(String attributeName, Attributes values, String uri) throws MissingRequiredAttributeException {
            if(!Arrays.asList(attributes).contains(attributeName)) {
                throw new MissingRequiredAttributeException(String.format(
                    "Cannot find attribute \"%s\" for HardwareType \"%s\"",
                    attributeName,
                    this.name()
                ));
            }

            return values.getValue(uri, attributeName);
        }

        public double getDouble(String attributeName, Attributes values, String uri) throws MissingRequiredAttributeException {
            return Double.parseDouble(get(attributeName, values, uri));
        }
    
        public int getInt(String attributeName, Attributes values, String uri) throws MissingRequiredAttributeException {
            return Integer.parseInt(get(attributeName, values, uri));
        }
    }

    private static final Map<String, HardwareType> tagToHardwareMap = Map.ofEntries(
        entry("AnalogInput",              HardwareType.ANALOG_INPUT),
        entry("LynxColorSensor",          HardwareType.COLOR_DISTANCE_SENSOR),
        entry("RevColorSensorV3",         HardwareType.COLOR_DISTANCE_SENSOR),
        entry("ContinuousRotationServo",  HardwareType.CR_SERVO),
        entry("DigitalDevice",            HardwareType.DIGITAL_DEVICE),
        entry("REV_VL53L0X_RANGE_SENSOR", HardwareType.DISTANCE_SENSOR),
        entry("LynxEmbeddedIMU",          HardwareType.IMU),
        entry("RevBlinkinLedDriver",      HardwareType.LED),
        entry("LynxModule",               HardwareType.LYNX_MODULE),
        entry("LynxUsbDevice",            HardwareType.LYNX_USB_DEVICE), // TODO: Add more tag-device pairs!
        entry("goBILDA5201SeriesMotor",   HardwareType.MOTOR),
        entry("goBILDA5202SeriesMotor",   HardwareType.MOTOR),
        entry("Motor",                    HardwareType.MOTOR),
        entry("NeveRest20Gearmotor",      HardwareType.MOTOR),
        entry("NeveRest3",                HardwareType.MOTOR),
        entry("NeveRest40Gearmotor",      HardwareType.MOTOR), 
        entry("NeveRest60Gearmotor",      HardwareType.MOTOR),
        entry("RevRobotics20HDHexMotor",  HardwareType.MOTOR),
        entry("RevRobotics40HDHexMotor",  HardwareType.MOTOR),
        entry("RevRoboticsCoreHexMotor",  HardwareType.MOTOR),
        entry("RevSPARKMini",             HardwareType.MOTOR), // Because its a DcMotorSimple
        entry("TetrixMotor",              HardwareType.MOTOR),
        entry("MotorController",          HardwareType.MOTOR_CONTROLLER),
        entry("Servo",                    HardwareType.SERVO),
        entry("ServoController",          HardwareType.SERVO_CONTROLLER),
        entry("RevTouchSensor",           HardwareType.TOUCH_SENSOR)
        // entry(),

    );
    
    private int lastLynxAddress = -1;
    private int parentAddress = -1;
    private SAXParserFactory inputFactory;
    private Exception bubbledException = null;

    public XmlHardwareFakeFactory() {
        inputFactory = SAXParserFactory.newInstance();
        inputFactory.setValidating(true);
    }

    public HardwareMap createHardwareMap(InputStream xmlStream) 
        throws SAXException, IOException, MissingRequiredAttributeException, 
            ParserConfigurationException, InterruptedException, RobotCoreException
    {
        final SAXParser reader = inputFactory.newSAXParser();
        final HardwareMap result = new HardwareMap(null, null); // Yes, the OpModeNotifier *shouldn't* be null, but it doesn't matter

        parentAddress = -1;
        lastLynxAddress = -1;
        reader.parse(xmlStream, new ConfigHandler(result));

        // Throwing any exception that was supposed to have been thrown
        if(bubbledException != null) {
            xmlStream.close();
            bubbledException = null;
            throw new SAXException("Error occurred during SAX XML parsing", bubbledException);
        }
        
        // Cleaning up
        xmlStream.close();
        return result;
    } 

    private HardwareDevice addHardwareFromCurrentTag(
        String name, 
        String uri, 
        Attributes attr, 
        HardwareMap map, 
        Deque<HardwareDevice> hierarchy
    ) throws  MissingRequiredAttributeException, InterruptedException, RobotCoreException {
        // Getting the current tag
        HardwareType type;
        if(!tagToHardwareMap.containsKey(name)) {
            type = HardwareType.UNKNOWN;
        } else {
            type = tagToHardwareMap.get(name);
        }

        // Adding the hardware based off the tag.
        final HardwareDevice result = createDevice(type, uri, attr, hierarchy);
        throwIfDoesNotContain(attr, new String[]{ "name" });
        map.put(attr.getValue(uri, "name"), result);
        return result;
    }

    private HardwareDevice createDevice(
        HardwareType type,
        String uri,
        Attributes attributes,
        Deque<HardwareDevice> hierarchy
    ) throws  MissingRequiredAttributeException, 
            InterruptedException, RobotCoreException
    {
        switch(type) {
            case CR_SERVO: { 
                throwIfDoesNotContain(attributes, type.attributes);
                return new CRServoImplExFake(new ContinuousServoData(
                    type.getDouble("rpm", attributes, uri), 
                    0, 
                    new PwmRange(
                        type.getDouble("pwmMin", attributes, uri), 
                        type.getDouble("pwmMin", attributes, uri)
                    )
                ), findServoController(hierarchy), type.getInt("port", attributes, uri));
            }

            case LYNX_MODULE: {
                // final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                throwIfDoesNotContain(attributes, type.attributes);
                final int port = type.getInt("port", attributes, uri);
                final LynxUsbDeviceImplFake usbDevice = findLynxUsbDevice(hierarchy);
                return usbDevice.getOrAddModule(
                    new LynxModuleDescription.Builder(port, port == parentAddress)
                        .setUserModule()
                        .build()
                );
            }

            case LYNX_USB_DEVICE: {
                throwIfDoesNotContain(attributes, type.attributes);
                parentAddress = type.getInt("parentModuleAddress", attributes, uri);
                final LynxUsbDeviceImplFake device = new LynxUsbDeviceImplFake();
                device.armOrPretend();
                return device;
            }

            case MOTOR: { 
                // final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                throwIfDoesNotContain(attributes, type.attributes);
                return new DcMotorImplExFake(new MotorData(
                    type.getDouble("rpm", attributes, uri), 
                    0, 
                    type.getDouble("ticksPerRev", attributes, uri)
                ), findDcMotorController(hierarchy), type.getInt("port", attributes, uri));
            }

            case MOTOR_CONTROLLER: 
                return new DcMotorControllerExFake(findLynxModule(hierarchy));

            case SERVO: { 
                // final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                throwIfDoesNotContain(attributes, type.attributes);
                return new ServoImplExFake(new PositionalServoData(
                    type.getDouble("rpm", attributes, uri), 
                    type.getDouble("turns", attributes, uri),
                    0, 
                    new PwmRange(
                        type.getDouble("pwmMin", attributes, uri), 
                        type.getDouble("pwmMin", attributes, uri)
                    )
                ), findServoController(hierarchy), type.getInt("port", attributes, uri));
            }

            case SERVO_CONTROLLER: 
                return new ServoControllerExFake();

            case UNKNOWN:
            default:
                System.err.println("WARNING: Attempted to create unexpected " + type.name());
                return null;
        }
    }

    /**
     * Finds the last DcMotorControllerExFake in the hierarchy. If none is 
     * found, creates a new one.
     * 
     * This operators on last-in, first-out; the HardwareDevice added last will
     * be the one found by this method.
     * 
     * @param hierarchy The order of elements the parser is nested in.
     * @return The last added DcMotorController ()
     */
    private DcMotorControllerExFake findDcMotorController(Deque<HardwareDevice> hierarchy) {
        for(final HardwareDevice superDevice : hierarchy) {
            if(superDevice instanceof DcMotorControllerExFake) {
                return (DcMotorControllerExFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new DcMotorControllerExFake(findLynxModule(hierarchy));
    }

    private ServoControllerExFake findServoController(Deque<HardwareDevice> hierarchy) {
        for(final HardwareDevice superDevice : hierarchy) {
            if(superDevice instanceof ServoControllerExFake) {
                return (ServoControllerExFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new ServoControllerExFake();
    }

    private LynxModuleHardwareFake findLynxModule(Deque<HardwareDevice> hierarchy) {
        for(final HardwareDevice superDevice : hierarchy) {
            if(superDevice instanceof LynxModuleHardwareFake) {
                return (LynxModuleHardwareFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new LynxModuleHardwareFake(findLynxUsbDevice(hierarchy), lastLynxAddress--, false, true);

    }

    private LynxUsbDeviceImplFake findLynxUsbDevice(Deque<HardwareDevice> hierarchy) {
        for(final HardwareDevice superDevice : hierarchy) {
            if(superDevice instanceof LynxUsbDeviceImplFake) {
                return (LynxUsbDeviceImplFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new LynxUsbDeviceImplFake();
    }

    private class ConfigHandler extends DefaultHandler {
        private final Deque<HardwareDevice> hierarchy = new LinkedBlockingDeque<>();
        private final HardwareMap result;
        private DcMotorImplExFake[] motors = new DcMotorImplExFake[0];
        // TODO: Add didigtal and analog inputs.

        public ConfigHandler(HardwareMap result) {
            this.result = result;
        }

        @Override
        public void startElement(String uri, String name, String unused, Attributes attr) {
            // Adding the current tag to the stack
            try {
                final HardwareDevice device = addHardwareFromCurrentTag(name, uri, attr, result, hierarchy);
                hierarchy.addFirst(device);
                if(device instanceof DcMotorImplExFake) {
                    motors = Arrays.copyOf(motors, motors.length + 1);
                    motors[motors.length - 1] = (DcMotorImplExFake) device;
                    findLynxUsbDevice(hierarchy).setMotors(motors); // FIXME: Horrible performance? O(3n^2)?
                } /* else if(device instanceof AnalogInput) {} // TODO: add digital and analog devices */
            } catch (Exception exc) {
                bubbledException = exc;
            }
        }

        @Override
        public void endElement(String uri, String name, String unused) {
            // Removing from hierarchy
            hierarchy.removeFirst();
        }
    }

    private void throwIfDoesNotContain(Attributes attr, String[] requiredAttributes) 
        throws MissingRequiredAttributeException
    {
        final boolean[] hasFound = new boolean[requiredAttributes.length];
        for(int i = 0; i < attr.getLength(); i++) {
            for(final String name : requiredAttributes) {
                if(!hasFound[i] && attr.getLocalName(i).equals(name)) {
                    hasFound[i] = true;
                }
            }
        }

        // Throwing if any are false
        for(int i = 0; i < requiredAttributes.length; i++) {
            if(!hasFound[i]) {
                throw new MissingRequiredAttributeException(String.format(
                    "Could not find required attribute \"%s\"", 
                    requiredAttributes[i]
                ));
            }
        }
    }

    public HardwareMap createHardwareMap(File file) 
        throws  IOException, MissingRequiredAttributeException, SAXException, 
            ParserConfigurationException, InterruptedException, RobotCoreException
    {
        final InputStream stream = new FileInputStream(file);
        final HardwareMap result = createHardwareMap(stream);
        stream.close();
        return result;
    }

    public HardwareMap createHardwareMap(String fileName) 
        throws  IOException, MissingRequiredAttributeException, SAXException, 
            ParserConfigurationException,InterruptedException, RobotCoreException
    {
        return createHardwareMap(new File(fileName));
    }

    public HardwareMap createHardwareMapFromContents(String xmlContents) 
        throws  IOException, MissingRequiredAttributeException, SAXException, 
            ParserConfigurationException, InterruptedException, RobotCoreException
    {
        final InputStream stream = new ByteArrayInputStream(xmlContents.getBytes(StandardCharsets.UTF_8));
        final HardwareMap result = createHardwareMap(stream);
        stream.close();
        return result;
    }
}