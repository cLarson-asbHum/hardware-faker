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

package clarson.ftc.faker.factory;

// TODO: Add more hardware!
import clarson.ftc.faker.*;
import clarson.ftc.faker.updater.ModularUpdater;
import clarson.ftc.faker.wrapper.ContinuousServoData;
import clarson.ftc.faker.wrapper.DigitalChannelData;
import clarson.ftc.faker.wrapper.MotorData;
import clarson.ftc.faker.wrapper.PositionalServoData;
import com.qualcomm.robotcore.exception.RobotCoreException;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.LynxModuleDescription;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import static com.qualcomm.robotcore.hardware.PwmControl.PwmRange;
import static java.util.Map.entry;    

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
        HardwareStack stack
    ) throws  MissingRequiredAttributeException, InterruptedException, RobotCoreException {
        // Getting the current tag
        HardwareType type;
        if(!tagToHardwareMap.containsKey(name)) {
            type = HardwareType.UNKNOWN;
        } else {
            type = tagToHardwareMap.get(name);
        }

        // Adding the hardware based off the tag.
        final HardwareDevice result = createDevice(type, uri, attr, stack);
        throwIfDoesNotContain(attr, new String[]{ "name" });
        map.put(attr.getValue(uri, "name"), result);
        return result;
    }

    private HardwareDevice createDevice(
        HardwareType type,
        String uri,
        Attributes attributes,
        HardwareStack stack
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
                ), stack.findServoController(), type.getInt("port", attributes, uri));
            }

            case LYNX_MODULE: {
                // final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                throwIfDoesNotContain(attributes, type.attributes);
                final int port = type.getInt("port", attributes, uri);
                final LynxUsbDeviceImplFake usbDevice = stack.findLynxUsbDevice();
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
                ), stack.findDcMotorController(), type.getInt("port", attributes, uri));
            }

            case MOTOR_CONTROLLER: 
                return new DcMotorControllerExFake(stack.findLynxModule());

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
                ), stack.findServoController(), type.getInt("port", attributes, uri));
            }

            case SERVO_CONTROLLER: 
                return new ServoControllerExFake();

            case UNKNOWN:
            default:
                System.err.println("WARNING: Attempted to create unexpected " + type.name());
                return null;
        }
    }

    private class ConfigHandler extends DefaultHandler {
        private final HardwareStack stack = new HardwareStack();
        private final HardwareMap result;
        private ArrayList<DcMotorImplExFake>      motors   = new ArrayList<>();
        private ArrayList<DigitalChannelImplFake> digitals = new ArrayList<>();
        private ArrayList<AnalogInputFake>        analogs  = new ArrayList<>();

        public ConfigHandler(HardwareMap result) {
            this.result = result;
        }

        @Override
        public void startElement(String uri, String name, String unused, Attributes attr) {
            // Adding the current tag to the stack
            try {
                final HardwareDevice device = addHardwareFromCurrentTag(name, uri, attr, result, stack);
                stack.push(device);
                if(device instanceof DcMotorImplExFake) {
                    motors.add((DcMotorImplExFake) device);
                    stack
                        .findLynxUsbDevice()
                        .setMotors(motors.toArray(new DcMotorImplExFake[0]));
                } else if(device instanceof DigitalChannelImplFake) {
                    digitals.add((DigitalChannelImplFake) device);
                    stack
                        .findLynxUsbDevice()
                        .setDigitalChannels(digitals.toArray(new DigitalChannelImplFake[0]));
                } else if(device instanceof AnalogInputFake) {
                    analogs.add((AnalogInputFake) device);
                    stack
                        .findLynxUsbDevice()
                        .setAnalogInputs(analogs.toArray(new AnalogInputFake[0]));
                }
            } catch (Exception exc) {
                bubbledException = exc;
            }
        }

        @Override
        public void endElement(String uri, String name, String unused) {
            // Removing from stack
            stack.pop();
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