/*
 * HardwareStack.java
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

import clarson.ftc.faker.DcMotorControllerExFake;
import clarson.ftc.faker.LynxModuleHardwareFake;
import clarson.ftc.faker.LynxUsbDeviceImplFake;
import clarson.ftc.faker.ServoControllerExFake;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.Deque;

public class HardwareStack {
    private final Deque<HardwareDevice> stack = new LinkedBlockingDeque<>();
    private int lastLynxAddress = -1;
    
    /**
     * Adds hardware to the stack. The stack follows the principle of first-in-last-out
     */
    public void push(HardwareDevice device) {
        stack.addFirst(device);
    }

    /**
     * Removes the most recently added hardware device (first-in-last-out).
     * 
     * @return Whether anything was actually removed
     */
    public boolean pop() {
        return stack.removeFirst() != null;
    }

    // TODO: Remember when we find a controller or module, so we don;t have to search
    //       Otherwise, we have quadratic complexity.

    /**
     * Finds the last DcMotorControllerExFake in the stack. If none is 
     * found, creates a new one.
     * 
     * This operators on last-in, first-out; the HardwareDevice added last will
     * be the one found by this method.
     * 
     * @param stack The order of elements the parser is nested in.
     * @return The last added DcMotorController ()
     */
    public DcMotorControllerExFake findDcMotorController() {
        for(final HardwareDevice superDevice : stack) {
            if(superDevice instanceof DcMotorControllerExFake) {
                return (DcMotorControllerExFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new DcMotorControllerExFake(findLynxModule());
    }

    public ServoControllerExFake findServoController() {
        for(final HardwareDevice superDevice : stack) {
            if(superDevice instanceof ServoControllerExFake) {
                return (ServoControllerExFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new ServoControllerExFake();
    }

    public LynxModuleHardwareFake findLynxModule() {
        for(final HardwareDevice superDevice : stack) {
            if(superDevice instanceof LynxModuleHardwareFake) {
                return (LynxModuleHardwareFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new LynxModuleHardwareFake(findLynxUsbDevice(), 
                lastLynxAddress--, false, true);

    }

    public LynxUsbDeviceImplFake findLynxUsbDevice() {
        for(final HardwareDevice superDevice : stack) {
            if(superDevice instanceof LynxUsbDeviceImplFake) {
                return (LynxUsbDeviceImplFake) superDevice;
            }
        }

        // Creating a new DcMotorController
        return new LynxUsbDeviceImplFake();
    }
}