import logging

logger = logging.getLogger(__name__)

class OtpProvider:
    def send_otp(self, phone_number: str, otp: str):
        raise NotImplementedError

class FakeOtpProvider(OtpProvider):
    def send_otp(self, phone_number: str, otp: str):
        # Outputs to logs which can be intercepted by tests or mailpit logs
        logger.info(f"FAKE_OTP_SENT: phone={phone_number}, otp={otp}")
