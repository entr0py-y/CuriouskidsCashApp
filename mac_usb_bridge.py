#!/usr/bin/env python3
import socket
import sys
import time
import glob
import serial
import serial.tools.list_ports

HOST = '0.0.0.0'
PORT = 9999
BAUD_RATES = [115200, 9600, 19200, 38400, 4800]

def find_serial_port():
    ports = serial.tools.list_ports.comports()
    for p in ports:
        if "usb" in p.device.lower() or "serial" in p.device.lower() or "cu." in p.device.lower() or "tty." in p.device.lower():
            if "bluetooth" not in p.device.lower() and "debug" not in p.device.lower() and "airdopes" not in p.device.lower():
                return p.device
    patterns = ['/dev/tty.usb*', '/dev/cu.usb*', '/dev/tty.wch*', '/dev/tty.SLAB*']
    for pat in patterns:
        matches = glob.glob(pat)
        if matches:
            return matches[0]
    return None

def main():
    port_name = find_serial_port()
    if not port_name:
        print("Searching for USB serial cash machine on Mac...")
        print("Please plug your USB cash machine into your Mac's USB port.")
        while not port_name:
            time.sleep(2)
            port_name = find_serial_port()

    print(f"Found USB Cash Machine Hardware at: {port_name}")

    ser = None
    for baud in BAUD_RATES:
        try:
            print(f"Attempting to open {port_name} at {baud} baud...")
            ser = serial.Serial(port_name, baud, timeout=1)
            print(f"Successfully opened {port_name} at {baud} baud!")
            break
        except Exception as e:
            print(f"Could not open at {baud} baud: {e}")

    if not ser:
        print("Failed to open serial port.")
        sys.exit(1)

    print(f"\n=======================================================")
    print(f" Mac USB Hardware Bridge Active on port {PORT}")
    print(f" Ready to stream real USB cash machine data to app!")
    print(f"=======================================================\n")

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((HOST, PORT))
    server_socket.listen(1)

    while True:
        try:
            client_socket, client_address = server_socket.accept()
            print(f"Connected to Android App from {client_address}!")
            print("Streaming real USB cash machine data...\n")

            while True:
                data = ser.read(1024)
                if data:
                    print(f"[Real Cash Machine Raw Data]: {data}")
                    client_socket.sendall(data)
                time.sleep(0.05)
        except (socket.error, BrokenPipeError):
            print("Client disconnected. Waiting for re-connection...")
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
