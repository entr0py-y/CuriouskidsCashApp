#!/usr/bin/env python3
import socket
import sys
import time
import glob
import serial
import serial.tools.list_ports

HOST = '0.0.0.0'
PORT = 9999
BAUD_RATE = 115200

def find_serial_port():
    ports = serial.tools.list_ports.comports()
    for p in ports:
        if "usb" in p.device.lower() or "serial" in p.device.lower() or "cu." in p.device.lower() or "tty." in p.device.lower():
            if "bluetooth" not in p.device.lower() and "debug" not in p.device.lower() and "airdopes" not in p.device.lower():
                return p.device
    # Fallback to glob search
    patterns = ['/dev/tty.usb*', '/dev/cu.usb*', '/dev/tty.wch*', '/dev/tty.SLAB*']
    for pat in patterns:
        matches = glob.glob(pat)
        if matches:
            return matches[0]
    return None

def main():
    port_name = find_serial_port()
    if not port_name:
        print("Searching for USB serial hardware on Mac...")
        print("Please plug your USB cash machine into your Mac's USB port.")
        while not port_name:
            time.sleep(2)
            port_name = find_serial_port()

    print(f"Found USB Cash Machine Hardware at: {port_name}")
    print(f"Opening {port_name} at {BAUD_RATE} baud...")

    try:
        ser = serial.Serial(port_name, BAUD_RATE, timeout=1)
    except Exception as e:
        print(f"Error opening serial port {port_name}: {e}")
        sys.exit(1)

    print(f"\n=======================================================")
    print(f" Mac USB Hardware Bridge Active on port {PORT}")
    print(f" Waiting for Android Studio Emulator to connect...")
    print(f"=======================================================\n")

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((HOST, PORT))
    server_socket.listen(1)

    while True:
        try:
            client_socket, client_address = server_socket.accept()
            print(f"Connected to Android Studio Emulator from {client_address}!")
            print("Streaming real USB cash machine hardware data to emulator...\n")

            while True:
                data = ser.read(1024)
                if data:
                    print(f"[Real Hardware Raw Byte Data]: {data}")
                    client_socket.sendall(data)
                time.sleep(0.05)
        except (socket.error, BrokenPipeError):
            print("Emulator disconnected. Waiting for re-connection...")
        except KeyboardInterrupt:
            print("\nStopping Mac USB Bridge.")
            break
        finally:
            try:
                client_socket.close()
            except:
                pass

    ser.close()
    server_socket.close()

if __name__ == "__main__":
    main()
