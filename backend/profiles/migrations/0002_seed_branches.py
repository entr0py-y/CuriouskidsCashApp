from django.db import migrations

def seed_branches(apps, schema_editor):
    Branch = apps.get_model('profiles', 'Branch')

    Branch.objects.get_or_create(
        name="Skyjumpers Delhi (Connaught Place)",
        defaults={
            "address": "Inner Circle, Connaught Place, New Delhi, Delhi 110001",
            "latitude": 28.631500,
            "longitude": 77.216700,
            "opening_hours": {
                "mon_fri": "11:00 - 21:00",
                "sat_sun": "10:00 - 22:00",
                "holidays": "10:00 - 22:00"
            },
            "contact_numbers": ["+911140001111", "+919810012345"],
            "facilities": ["Trampoline Park", "Arcade Games", "Party Rooms", "Food Court", "Valet Parking"],
            "images": ["https://skyjumpers-media.s3.amazonaws.com/branches/delhi_cp.jpg"],
            "is_active": True
        }
    )

    Branch.objects.get_or_create(
        name="Skyjumpers Gurgaon (Cyber Hub)",
        defaults={
            "address": "DLF Cyber City, Phase 2, Gurugram, Haryana 122002",
            "latitude": 28.495000,
            "longitude": 77.089500,
            "opening_hours": {
                "mon_fri": "11:00 - 22:00",
                "sat_sun": "10:00 - 23:00",
                "holidays": "10:00 - 23:00"
            },
            "contact_numbers": ["+911244002222", "+919810054321"],
            "facilities": ["Trampoline Park", "VR Arena", "Laser Tag", "Gourmet Cafe", "VIP Lounge"],
            "images": ["https://skyjumpers-media.s3.amazonaws.com/branches/gurgaon_cyberhub.jpg"],
            "is_active": True
        }
    )

def unseed_branches(apps, schema_editor):
    Branch = apps.get_model('profiles', 'Branch')
    Branch.objects.filter(name__in=[
        "Skyjumpers Delhi (Connaught Place)",
        "Skyjumpers Gurgaon (Cyber Hub)"
    ]).delete()

class Migration(migrations.Migration):

    dependencies = [
        ('profiles', '0001_initial'),
    ]

    operations = [
        migrations.RunPython(seed_branches, unseed_branches),
    ]
