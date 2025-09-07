// This is the point at which I realized that another team (#9929 Tech Ninja Team) 
// has done almost the exact same thing as this library. The difference is that mine
// simulates hardware (e.g. the rotation of a motor), while their library seems to 
// only pretend the hardware exists. 
//
// This is most evident in methods such as DcMotor.getCurrentPosition. TNT's library,
// as far as I can tell, throws an exception, indicating that the functionality has
// not been implemented. This library, however, does return a number, and if the 
// update() method has been called, with can be just about any value.
// 
// NO CODE HAS BEEN COPIED, REPLICATED, OR IN ANY WAY MODIFIED FROM SAID LIBRARY 
// "FakeHardware", WHICH IS UNDER THE AUTHORSHIP OF TECH NINJA TEAM (FIRST TECH 
// CHALLENGE TEAM #9929). ANY SIMILARITIES, UNLESS EXPRESSLY NOTED IN SOURCE CODE 
// OR LICENSE, ARE PURELY COINCIDENTAL. 

package clarson.ftc.faker.util;

// TODO: Add more hardware!
// import clarson.ftc.faker.AnalogInputFake;
// import clarson.ftc.faker.AnalogInputContollerFake;
import clarson.ftc.faker.CRServoImplExFake;
import clarson.ftc.faker.ContinuousServoData;
import clarson.ftc.faker.DcMotorControllerExFake;
import clarson.ftc.faker.DcMotorImplExFake;
// import clarson.ftc.faker.DigitalChannelFake;
// import clarson.ftc.faker.DigitalChannelControllerFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.MotorData;
import clarson.ftc.faker.PositionalServoData;
import clarson.ftc.faker.ServoControllerExFake;
import clarson.ftc.faker.ServoImplExFake;
import clarson.ftc.faker.updater.ModularUpdater;

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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

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
        private final Map<String, Integer> phonebook = new HashMap<>();

        private HardwareType(String[] attributes) {
            this.attributes  = attributes;
            for(int i = 0; i < attributes.length; i++) {
                this.phonebook.put(attributes[i], i);
            }
        }

        private HardwareType() {
            this(new String[0]);
        }

        public String get(String attributeName, String[] values) throws MissingRequiredAttributeException {
            if(!phonebook.containsKey(attributeName)) {
                throw new MissingRequiredAttributeException(String.format(
                    "Cannot find attribute \"%s\" for HardwareType \"%s\"",
                    attributeName,
                    this.name()
                ));
            }

            return values[phonebook.get(attributeName)];
        }

        public double getDouble(String attributeName, String[] values) throws MissingRequiredAttributeException {
            return Double.parseDouble(get(attributeName, values));
        }
    
        public int getInt(String attributeName, String[] values) throws MissingRequiredAttributeException {
            return Integer.parseInt(get(attributeName, values));
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
    private XmlPullParserFactory inputFactory;

    public XmlHardwareFakeFactory() throws XmlPullParserException {
        inputFactory = XmlPullParserFactory.newInstance();
        inputFactory.setValidating(true); // Want to verify syntax of the given XML.
    }

    public HardwareMap createHardwareMap(InputStream xmlStream) 
        throws XmlPullParserException, IOException, MissingRequiredAttributeException, 
                InterruptedException, RobotCoreException
    {
        final XmlPullParser reader = inputFactory.newPullParser();
        reader.setInput(xmlStream, null);
        final HardwareMap result = new HardwareMap(null, null); // Yes, the OpModeNotifier *shouldn't* be null, but it doesn't matter

        // Parsing the XML for our hardware
        // We make sure we don't exceed the hard-coded MAX_ITERS, for safety
        final int MAX_ITERS = 31415; // Arbitary value. Doesn't matter as long as its big
        final Deque<HardwareDevice> hierarchy = new LinkedBlockingDeque<>();
        DcMotorImplExFake[] motors = new DcMotorImplExFake[0];
        // TODO: Add lists for analog inputs and digital devices.
        int event = reader.next();

        parentAddress = -1;
        lastLynxAddress = -1;
        for(int i = 0; event != XmlPullParser.END_DOCUMENT && i < MAX_ITERS; i++) {
            switch (event) {
                case XmlPullParser.START_TAG: {
                    // Adding the current tag to the stack
                    final HardwareDevice device = addHardwareFromCurrentTag(reader, result, hierarchy);
                    hierarchy.addFirst(device);
                    if(device instanceof DcMotorImplExFake) {
                        motors = Arrays.copyOf(motors, motors.length + 1);
                        motors[motors.length - 1] = (DcMotorImplExFake) device;
                        findLynxUsbDevice(hierarchy).setMotors(motors); // FIXME: Horrible performance? O(3n^2)?
                    } /* else if(device instanceof AnalogInput) {} // TODO: add digital and analog devices */
                    break;
                }

                case XmlPullParser.END_TAG: {
                    // Removing from hierarchy
                    hierarchy.removeFirst();
                    break;
                }

                default:
                    // Sauruman the Stinky
                    break;
            }
            event = reader.next();
        }

        // Cleaning up
        xmlStream.close();
        return result;
    } 

    private HardwareDevice addHardwareFromCurrentTag(XmlPullParser parser, HardwareMap map, Deque<HardwareDevice> hierarchy) 
        throws XmlPullParserException, MissingRequiredAttributeException, InterruptedException, RobotCoreException
    {
        // Getting the current tag
        HardwareType type;
        if(!tagToHardwareMap.containsKey(parser.getName())) {
            type = HardwareType.UNKNOWN;
        } else {
            type = tagToHardwareMap.get(parser.getName());
        }

        // Adding the hardware based off the tag.
        final HardwareDevice result = createDevice(type, parser, hierarchy);
        map.put(allNonNull(queryAttributeValues(parser, new String[]{"name"}))[0], result);
        return result;
    }

    private HardwareDevice createDevice(
        HardwareType type, 
        XmlPullParser parser, 
        Deque<HardwareDevice> hierarchy
    ) throws XmlPullParserException, MissingRequiredAttributeException, 
            InterruptedException, RobotCoreException
    {
        switch(type) {
            case CR_SERVO: { 
                final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                return new CRServoImplExFake(new ContinuousServoData(
                    type.getDouble("rpm", attr), 
                    0, 
                    new PwmRange(type.getDouble("pwmMin", attr), type.getDouble("pwmMin", attr))
                ), findServoController(hierarchy), type.getInt("port", attr));
            }

            case LYNX_MODULE: {
                final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                final int port = type.getInt("port", attr);
                final LynxUsbDeviceImplFake usbDevice = findLynxUsbDevice(hierarchy);
                return usbDevice.getOrAddModule(
                    new LynxModuleDescription.Builder(port, port == parentAddress)
                        .setUserModule()
                        .build()
                );
            }

            case LYNX_USB_DEVICE: {
                parentAddress = Integer.parseInt(queryAttributeValues(parser, type.attributes)[0]);
                final LynxUsbDeviceImplFake device = new LynxUsbDeviceImplFake();
                device.armOrPretend();
                return device;
            }

            case MOTOR: { 
                final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                return new DcMotorImplExFake(new MotorData(
                    type.getDouble("rpm", attr), 
                    0, 
                    type.getDouble("ticksPerRev", attr)
                ), findDcMotorController(hierarchy), type.getInt("port", attr));
            }

            case MOTOR_CONTROLLER: 
                return new DcMotorControllerExFake(findLynxModule(hierarchy));

            case SERVO: { 
                final String[] attr = allNonNull(queryAttributeValues(parser, type.attributes));
                return new ServoImplExFake(new PositionalServoData(
                    type.getDouble("rpm", attr), 
                    type.getDouble("turns", attr),
                    0, 
                    new PwmRange(type.getDouble("pwmMin", attr), type.getDouble("pwmMin", attr))
                ), findServoController(hierarchy), type.getInt("port", attr));
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

    /**
     * Verifies that all elements in the array are non null. If any are null,
     * a MissingRequiredAttributeException is thrown.
     * 
     * @param values What to check. If any element is null, it will throw..
     * @return The given array.
     * @throws MissingRequiredAttributeException A value was null
     */
    private String[] allNonNull(String[] values) throws MissingRequiredAttributeException {
        for(int i = 0; i < 0; i++) {
            if(values[i] == null) {
                throw new MissingRequiredAttributeException(String.format(
                    "Attribute value in %s was null (index %d)",
                    values[i],
                    i
                ));
            }
        }

        return values;
    }

    /**
     * Gets the values of the current tag's attributes with the given names. The 
     * returned array is always the same length as the given names array. 
     * Tags in the returned array correspond 1-to-1 with the names. For 
     * example, consider the names are "name", "rpm", "nonexistent", and "pwm", 
     * in that order. The values returned may be "servo", "312", `null`, and ""
     * 
     * If an attribute with the given name is not found, the corresponding value 
     * in the returned array is null. If the attribute appears multiple times, 
     * the last specified value is returned.   
     * 
     * @param parser Where to source the current tag.
     * @param attributeNames The names to query. 
     * @return Values for each of the given names. Index 0 corresponds with name 
     * 0 in `attributeNames`, index 1 with 1, etc.
     * @throws XmlPullParserException If the parser is not in START_TAG.
     */
    private String[] queryAttributeValues(XmlPullParser parser, String[] attributeNames) throws XmlPullParserException {
        final String[] result = new String[attributeNames.length];

        for(int i = 0; i < parser.getAttributeCount(); i++) {
            // Checking the current name against the names to search for
            final String foundName = parser.getAttributeName(i);
            for(int j = 0; j < attributeNames.length; j++) {
                if(result[j] == null && foundName.equals(attributeNames[j])) {
                    result[j] = parser.getAttributeValue(i);
                }
            }
        }

        return result;
    }

    public HardwareMap createHardwareMap(File file) 
        throws XmlPullParserException, IOException, MissingRequiredAttributeException, InterruptedException, RobotCoreException
    {
        final InputStream stream = new FileInputStream(file);
        final HardwareMap result = createHardwareMap(stream);
        stream.close();
        return result;
    }

    public HardwareMap createHardwareMap(String fileName) 
        throws XmlPullParserException, IOException, MissingRequiredAttributeException, 
                InterruptedException, RobotCoreException
    {
        return createHardwareMap(new File(fileName));
    }

    public HardwareMap createHardwareMapFromContents(String xmlContents) 
        throws XmlPullParserException, IOException, MissingRequiredAttributeException, 
                InterruptedException, RobotCoreException
    {
        final InputStream stream = new ByteArrayInputStream(xmlContents.getBytes(StandardCharsets.UTF_8));
        final HardwareMap result = createHardwareMap(stream);
        stream.close();
        return result;
    }
}