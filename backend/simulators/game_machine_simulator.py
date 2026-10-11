import sys
import time
import uuid
from simulators.game_machine_sdk import GameMachineSDK

class GameMachineSimulator:
    def __init__(self, machine_id: str, secret: str, game_code: str = "TEKKEN", base_url: str = "http://localhost:8000"):
        self.sdk = GameMachineSDK(machine_id, secret, base_url)
        self.game_code = game_code
        self.state = "IDLE"

    def simulate_qr_scan(self, qr_token: str) -> dict:
        print(f"\n[MACHINE STATE: {self.state}] -> Scanning QR Code...")
        self.state = "SCANNED"

        # 1. Identify User
        id_res = self.sdk.identify(qr_token)
        print(f"[IDENTIFY RESPONSE]: {id_res}")

        if id_res.get("eligible") is not True:
            self.state = "IDLE"
            reason = id_res.get("reason") or id_res.get("error", {}).get("message", "DECLINED")
            print(f"[DISPLAY SCREEN]: DECLINED - {reason}")
            return id_res

        name = id_res.get("display_name", "Customer")
        bal_rupees = id_res.get("wallet_total_paise", 0) / 100.0
        print(f"[DISPLAY SCREEN]: Hello {name}, Balance ₹{bal_rupees:.2f}")

        # 2. Charge Game
        self.state = "CHARGING"
        idem_key = f"SIM_CHARGE_{uuid.uuid4().hex[:12]}"
        charge_res = self.sdk.charge(qr_token, self.game_code, idem_key)
        print(f"[CHARGE RESPONSE]: {charge_res}")

        if charge_res.get("approved") is True:
            self.state = "PLAYING"
            charged_paise = charge_res.get("charged_paise", 0)
            bal_after = charge_res.get("balance_after_paise", 0) / 100.0
            print(f"[DISPLAY SCREEN]: GAME STARTED! Charged ₹{charged_paise/100:.2f}. Balance left: ₹{bal_after:.2f}")
            time.sleep(2)
            self.state = "IDLE"
            print(f"[MACHINE STATE]: Game finished. Returning to IDLE.")
        else:
            self.state = "IDLE"
            err = charge_res.get("error", {}).get("message", "CHARGE_DECLINED")
            print(f"[DISPLAY SCREEN]: DECLINED - {err}")

        return charge_res

    def simulate_hardware_fault(self, qr_token: str):
        print(f"\n[SIMULATING HARDWARE FAULT FLOW]...")
        idem_key = f"SIM_FAULT_{uuid.uuid4().hex[:12]}"
        charge_res = self.sdk.charge(qr_token, self.game_code, idem_key)

        if charge_res.get("approved") is True:
            txn_id = charge_res["transaction_id"]
            print(f"[FAULT DETECTED]: Motor/Relay failed after charge! Transaction ID: {txn_id}")
            self.state = "FAULT"

            refund_res = self.sdk.refund(txn_id, "MOTOR_JAMMED")
            print(f"[REFUND RESPONSE]: {refund_res}")
            self.state = "IDLE"
            print(f"[DISPLAY SCREEN]: Game fault. Amount refunded to wallet.")

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python game_machine_simulator.py <machine_id> <secret> <qr_token>")
        sys.exit(1)

    m_id = sys.argv[1]
    m_sec = sys.argv[2]
    qr = sys.argv[3]

    sim = GameMachineSimulator(m_id, m_sec)
    sim.simulate_qr_scan(qr)
