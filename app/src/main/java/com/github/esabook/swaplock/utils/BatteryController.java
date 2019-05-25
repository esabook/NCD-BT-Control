package com.github.esabook.swaplock.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class BatteryController {

    int[] individualRelayStatus = {0, 0, 0, 0, 0, 0, 0, 0, 0};    //this holds the array of indivdual relay status returned from the board
    private String mBtAddress;
    private BtModuleEventListener mBtEventListener = (device, event) -> {
    };
    private BluetoothDevice mBtDevice;
    private BluetoothSocket btSocket;
    private BluetoothAdapter mBtAdapter;

    public String getBtAddress() {
        return mBtAddress;
    }

    public BtModuleEventListener getBtEventListener() {
        return mBtEventListener;
    }

    public BluetoothDevice getBtDevice() {
        return mBtDevice;
    }

    public BluetoothSocket getBtSocket() {
        return btSocket;
    }

    public BluetoothAdapter getmBtAdapter() {
        return mBtAdapter;
    }


    public BatteryController(String btAddress, BtModuleEventListener listener) {
        mBtAddress = btAddress;
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (listener == null) {
            mBtEventListener = (device, event) -> {
            };
        }
    }

    public BatteryController(BluetoothDevice btDevice, BtModuleEventListener listener) {
        this(btDevice.getAddress(), listener);
    }


    /**
     *
     */
    public enum BtModuleEvent {
        CONNECT(1),
        DISCONNECT(0),
        RELAY_ON(2),
        RELAY_OFF(3);


        BtModuleEvent(int i) {

        }
    }

    /**
     *
     */
    public interface BtModuleEventListener {
        void event(BluetoothDevice device, BtModuleEvent event);
    }


    /**
     * @param command
     * @return
     */
    public boolean sendCommand(Byte[] command) {
        try {
            byte[] recievedData = (new SendCommandBT().execute(command).get());
            if (recievedData != null) {
                if (recievedData[2] == 85 || recievedData[2] == 86 || recievedData[0] == 85 || recievedData[0] == 86) {
                    return true;
                } else {
                    System.out.println(Arrays.toString(recievedData));
                    System.out.println("Response from controller invalid");
                    return false;
                }
            } else {
                return false;
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return false;
        } catch (ExecutionException e1) {
            e1.printStackTrace();
            return false;
        }
    }


    //Bluetooth Connect
    public boolean connect(String address) {
        mBtAddress = address;
        Byte[] command = new Byte[5];
        command[0] = (byte) 170;
        command[1] = (byte) 2;
        command[2] = (byte) 254;
        command[3] = (byte) 33;
        command[4] = (byte) ((command[0] + command[1] + command[2] + command[3]) & 255);


        System.out.println("about to trigger connection Bluetooth");
        try {
            //Send command to Async Task(blocks until data is returned or connection fails)
            byte[] recievedData = (new SendCommandBT().execute(command).get());

            if (recievedData != null) {
                if (recievedData.length > 1) {
                    System.out.println("recieved: " + Arrays.toString(recievedData));
                    if (recievedData[0] == 85 || recievedData[0] == 86 || recievedData[2] == 85 || recievedData[2] == 86) {
//                        connected = true;
                        mBtEventListener.event(mBtDevice, BtModuleEvent.CONNECT);
                        return true;
                    }
                } else {
                    if (recievedData[0] == 85 || recievedData[0] == 86) {
//                        connected = true;
                        mBtEventListener.event(mBtDevice, BtModuleEvent.DISCONNECT);
                        return true;
                    } else {
                        return false;
                    }
                }

            } else {
                System.out.println("connect returning false2 Bluetooth");
                disconnect();
                return false;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return false;

    }

    public void disconnect() {

        if (btSocket != null) {
            try {
                btSocket.close();
                btSocket = null;
                mBtEventListener.event(mBtDevice, BtModuleEvent.DISCONNECT);
                System.out.println("Bluetooth Socket Closed");
            } catch (IOException e) {
                System.out.println("Exception closing Bluetooth Socket");
                e.printStackTrace();
            }
        }


    }

    /**
     *
     */
    public class SendCommandBT extends AsyncTask<Byte, Integer, byte[]> {

        @Override
        protected byte[] doInBackground(Byte... command) {

            //Socket not yet created so create it
            if (btSocket == null) {
                //No Address so stop
                if (mBtAddress == null) {
                    System.out.println("No Selected device");
                    return null;
                }

                mBtDevice = mBtAdapter.getRemoteDevice(mBtAddress);
                try {
                    btSocket = mBtDevice.createRfcommSocketToServiceRecord(java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                    System.out.println("btSocket object created");
                } catch (IOException e) {
                    //Could not create socket so stop
                    System.out.println("Exception creating socket");
                    e.printStackTrace();
                    btSocket = null;
                    return null;
                }
            }
            //Socket is not yet connected so connect it
            if (!btSocket.isConnected()) {
                try {
                    //					btAdapter.cancelDiscovery();
                    btSocket.connect();
                    System.out.println("Connected");
                    //Sleep required after connection due to trash data bluetooth module puts out to chip after connection.
                    Thread.sleep(500);
                } catch (IOException e) {
                    //Could not connect so stop
                    System.out.println("could not connect bt");
                    e.printStackTrace();
                    disconnect();
                    btSocket = null;
                    return null;
                } catch (InterruptedException e) {
                    System.out.println("Exception on Sleep");
                    e.printStackTrace();
                    disconnect();
                    return null;
                }
            }

            //send the command
            //Generate byte command to be sent
            byte[] sendCommand = new byte[command.length];

            for (int i = 0; i < command.length; i++) {
                sendCommand[i] = command[i];
            }

            //send the command
            try {
                System.out.println("Sending: " + Arrays.toString(sendCommand));
                btSocket.getOutputStream().write(sendCommand);

                //wait for a response
                long startTime = System.currentTimeMillis();
                long timeout = 3000;
                while (System.currentTimeMillis() < (startTime + timeout)) {
                    if (btSocket.getInputStream().available() != 0) {
                        break;
                    }
                    System.out.println("Bytes in inputStream: " + btSocket.getInputStream().available());
                    System.out.println("timeout: " + (System.currentTimeMillis() > (startTime + timeout)));
                    Thread.sleep(100);
                }

                //Check that we got a response
                if (btSocket.getInputStream().available() == 0) {
                    //No Data returned so return null
                    System.out.println("No Data Returned here");
                    disconnect();
                    return null;

                }
                Thread.sleep(100);
                //Success
                byte[] returnData = new byte[btSocket.getInputStream().available()];
                btSocket.getInputStream().read(returnData);
                System.out.println("Received: " + Arrays.toString(returnData) + "After " + (System.currentTimeMillis() - startTime) + " ms");
                return returnData;

            } catch (IOException e) {
                System.out.println("Exception reading or writing to Bluetooth Socket");
                e.printStackTrace();
                disconnect();
                return null;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            } catch (NullPointerException e){
                return null;
            }
        }

    }

    /**
     * @return
     */
    public boolean checkPWM() {
        System.out.println("Checking PWM");
        Byte[] command = new Byte[6];
        command[0] = (byte) 170;
        command[1] = (byte) 3;
        command[2] = (byte) 254;
        command[3] = (byte) 53;
        command[4] = (byte) 244;
        command[5] = (byte) ((command[0] + command[1] + command[2] + command[3] + command[4]) & 255);


        try {
            byte[] deviceType = (new SendCommandBT().execute(command).get());
            if (deviceType != null) {
                //                        pwm = true;
//                        pwm = false;
                return (deviceType[2] & 2) == 2;

            } else {
//                    pwm = false;
                return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    //jacob built
    public Byte queryBoardRelayBankStatus(int bank) {

        Byte[] command = new Byte[6];
        command[0] = (byte) 170;
        command[1] = (byte) 3;
        command[2] = (byte) 254;
        command[3] = (byte) 124;
        command[4] = (byte) bank;
        command[5] = (byte) ((command[0] + command[1] + command[2] + command[3] + command[4]) & 255);

        try {
            byte[] recievedData = (new SendCommandBT().execute(command).get());
            if (recievedData != null) {
                System.out.println("Returned Data: " + Arrays.toString(recievedData));
                if (recievedData[1] == 1) {
                    return recievedData[2];
                } else {
                    return recievedData[0];
                }
            } else {
                disconnect();
                return null;
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return null;
        } catch (ExecutionException e1) {
            e1.printStackTrace();
            return null;
        }

    }

    /**
     * @param bank
     * @return
     */
    public int[] getBankStatus(int bank) {
        Byte statusByte = queryBoardRelayBankStatus(bank);
        int status = 0;
        if (statusByte != null) {
            status = statusByte;
            //this line makes the negative return values that android sometimes returns and makes it positive
            if (status < 0) {
                status = status + 256;
            }
            individualRelayStatus[8] = status;
            if (status > 127) {
                individualRelayStatus[7] = 128;
                status = status - 128;
            } else {
                individualRelayStatus[7] = 0;
            }
            if (status > 63) {
                individualRelayStatus[6] = 64;
                status = status - 64;
            } else {
                individualRelayStatus[6] = 0;
            }
            if (status > 31) {
                individualRelayStatus[5] = 32;
                status = status - 32;
            } else {
                individualRelayStatus[5] = 0;
            }
            if (status > 15) {
                individualRelayStatus[4] = 16;
                status = status - 16;
            } else {
                individualRelayStatus[4] = 0;
            }
            if (status > 7) {
                individualRelayStatus[3] = 8;
                status = status - 8;
            } else {
                individualRelayStatus[3] = 0;
            }
            if (status > 3) {
                individualRelayStatus[2] = 4;
                status = status - 4;
            } else {
                individualRelayStatus[2] = 0;
            }
            if (status > 1) {
                individualRelayStatus[1] = 2;
                status = status - 2;
            } else {
                individualRelayStatus[1] = 0;
            }
            if (status > 0) {
                individualRelayStatus[0] = 1;
            } else {
                individualRelayStatus[0] = 0;
            }
            return individualRelayStatus;
        } else {
            return null;
        }


    }


}
