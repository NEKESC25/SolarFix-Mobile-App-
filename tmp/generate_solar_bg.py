import math
import subprocess
import os

width = 1080
height = 1920

# PPM P6 format generator
# We will create an image buffer
print(f"Generating solar system background: {width}x{height}...")

# Preallocate bytearray for RGB
pixels = bytearray(width * height * 3)

def clamp(val, min_v=0, max_v=255):
    return max(min_v, min(max_v, int(val)))

# 1. Sky & Atmosphere base (y from 0 to 800)
# Deep sky blue to warm solar golden amber gradient
for y in range(height):
    row_offset = y * width * 3
    # normalized y
    ny = y / height
    
    # Base gradient: deep twilight/space blue at very top, fading into bright solar sky,
    # transitioning into solar panel rooftop in middle, and technician workbench at bottom
    if y < 650:
        # Sky: blue to bright horizon with sunlight
        t = y / 650.0
        # Deep royal blue to atmospheric sky with sun warmth
        r = 15 + t * 45
        g = 35 + t * 90
        b = 75 + t * 140
        # Add sun glare near (x=680, y=280)
        sun_x, sun_y = 680, 260
    elif y < 1350:
        # Solar Panel Rooftop section
        t = (y - 650) / 700.0
        # Dark crystalline silicon blue-black with slate tones
        r = 16 + t * 12
        g = 28 + t * 18
        b = 52 + t * 24
    else:
        # Technician Workbench & Equipment section
        t = (y - 1350) / 570.0
        # Industrial dark slate with amber tool reflections
        r = 18 + t * 16
        g = 24 + t * 20
        b = 36 + t * 22
        
    for x in range(width):
        idx = row_offset + x * 3
        pixels[idx] = clamp(r)
        pixels[idx + 1] = clamp(g)
        pixels[idx + 2] = clamp(b)

# 2. Add Sun & Radiant Golden Rays (Sky)
sun_cx, sun_cy = 720, 220
for y in range(max(0, sun_cy - 260), min(height, sun_cy + 260)):
    dy = y - sun_cy
    for x in range(max(0, sun_cx - 360), min(width, sun_cx + 360)):
        dx = x - sun_cx
        dist = math.sqrt(dx * dx + dy * dy)
        if dist < 320:
            angle = math.atan2(dy, dx)
            # Ray pulsation
            ray = (math.cos(angle * 14) + 1.0) * 0.5
            glare = (1.0 - dist / 320.0) ** 1.8
            boost = glare * (0.6 + 0.4 * ray) * 160
            
            idx = (y * width + x) * 3
            pixels[idx] = clamp(pixels[idx] + boost * 1.0)        # Gold R
            pixels[idx + 1] = clamp(pixels[idx + 1] + boost * 0.78) # Gold G
            pixels[idx + 2] = clamp(pixels[idx + 2] + boost * 0.35) # Gold B

# 3. Solar Panels Array (Perspective Grid from y=480 to y=1380)
# Multiple crystalline silicon photovoltaic panels with silver frame & busbars
panel_top = 480
panel_bottom = 1380
for y in range(panel_top, panel_bottom):
    ny = (y - panel_top) / (panel_bottom - panel_top)
    # Perspective widening
    left_x = int(40 - ny * 30)
    right_x = int(width - 40 + ny * 30)
    row_w = right_x - left_x
    
    # Panel vertical rows (e.g. 4 rows of panels)
    panel_row_h = (panel_bottom - panel_top) / 4.0
    panel_row_idx = int((y - panel_top) / panel_row_h)
    row_local_y = (y - panel_top) % panel_row_h
    is_panel_h_border = (row_local_y < 8) or (row_local_y > panel_row_h - 8)
    
    # Busbars horizontal within cell
    cell_h = panel_row_h / 6.0
    cell_local_y = row_local_y % cell_h
    is_busbar_h = (cell_local_y < 2)
    
    for x in range(max(0, left_x), min(width, right_x)):
        nx = (x - left_x) / float(row_w)
        # 3 panel columns across
        col_w = row_w / 3.0
        col_local_x = (x - left_x) % col_w
        is_panel_v_border = (col_local_x < 8) or (col_local_x > col_w - 8)
        
        # 4 cells per panel column
        cell_w = col_w / 4.0
        cell_local_x = col_local_x % cell_w
        is_busbar_v = (cell_local_x < 2)
        
        idx = (y * width + x) * 3
        
        if is_panel_h_border or is_panel_v_border:
            # Silver Aluminum Frame
            frame_shine = 180 + int(math.sin(x * 0.05 + y * 0.05) * 30)
            pixels[idx] = clamp(frame_shine * 0.85)
            pixels[idx + 1] = clamp(frame_shine * 0.9)
            pixels[idx + 2] = clamp(frame_shine * 1.0)
        elif is_busbar_h or is_busbar_v:
            # Silver conductive busbar lines
            pixels[idx] = 160
            pixels[idx + 1] = 185
            pixels[idx + 2] = 220
        else:
            # Deep blue-purple monocrystalline anti-reflective wafer
            # subtle silicon crystal grain
            grain = ((x ^ y) % 17) * 2
            # subtle sunlight sheen
            sheen = int(max(0, 1.0 - abs(nx - 0.65) * 2.0) * 45)
            
            pixels[idx] = clamp(14 + grain + sheen * 0.6)
            pixels[idx + 1] = clamp(26 + grain + sheen * 0.8)
            pixels[idx + 2] = clamp(58 + grain * 2 + sheen * 1.2)

# 4. Solar Inverter Station (Left side, y=750 to 1250, x=60 to 340)
inv_x1, inv_y1 = 80, 780
inv_x2, inv_y2 = 340, 1260
for y in range(inv_y1, inv_y2):
    for x in range(inv_x1, inv_x2):
        idx = (y * width + x) * 3
        # Inverter chassis: sleek matte white/platinum with dark accents
        border = (x - inv_x1 < 10) or (inv_x2 - x < 10) or (y - inv_y1 < 10) or (inv_y2 - y < 10)
        if border:
            pixels[idx] = 70
            pixels[idx + 1] = 80
            pixels[idx + 2] = 95
        else:
            # LCD Display area (y: 840 to 960, x: 120 to 300)
            if 840 <= y <= 960 and 120 <= x <= 300:
                # Digital screen glow (cyan / solar green)
                pixels[idx] = 8
                pixels[idx + 1] = 38
                pixels[idx + 2] = 48
                # Screen border
                if y == 840 or y == 960 or x == 120 or x == 300:
                    pixels[idx] = 15
                    pixels[idx + 1] = 60
                    pixels[idx + 2] = 75
                # Status LED bars
                if 870 <= y <= 890 and 140 <= x <= 280:
                    pixels[idx] = 20
                    pixels[idx + 1] = 220 # Bright green LED
                    pixels[idx + 2] = 100
            elif 1000 <= y <= 1180 and (y % 16 < 4):
                # Inverter cooling heat sink fins
                pixels[idx] = 40
                pixels[idx + 1] = 45
                pixels[idx + 2] = 55
            else:
                # White/silver chassis with soft shadow
                pixels[idx] = 180
                pixels[idx + 1] = 190
                pixels[idx + 2] = 200

# 5. Technician Tools (Foreground Workbench: y=1360 to 1900)
# A: Digital Multimeter (Yellow & Black case, x=120 to 380, y=1440 to 1860)
mm_x1, mm_y1 = 120, 1450
mm_x2, mm_y2 = 360, 1840
for y in range(mm_y1, mm_y2):
    for x in range(mm_x1, mm_x2):
        idx = (y * width + x) * 3
        # Rounded bevel
        edge_dist = min(x - mm_x1, mm_x2 - x, y - mm_y1, mm_y2 - y)
        if edge_dist < 14:
            # Heavy-duty rubber shock bumper (Bright Solar Yellow)
            pixels[idx] = 245
            pixels[idx + 1] = 185
            pixels[idx + 2] = 20
        else:
            # Body: Dark charcoal
            pixels[idx] = 30
            pixels[idx + 1] = 34
            pixels[idx + 2] = 40
            
            # LCD screen on multimeter (y: 1490 to 1590, x: 150 to 330)
            if 1490 <= y <= 1590 and 150 <= x <= 330:
                pixels[idx] = 155
                pixels[idx + 1] = 175
                pixels[idx + 2] = 145 # Classic backlit LCD greenish gray
                # 7-segment digital display readout "580.4 V"
                if 1525 <= y <= 1565 and 180 <= x <= 300:
                    if (x % 24 < 14) and (y % 14 < 10):
                        pixels[idx] = 25
                        pixels[idx + 1] = 35
                        pixels[idx + 2] = 25
            # Rotary Dial (center: 240, 1690, radius: 45)
            dx = x - 240
            dy = y - 1690
            d = math.sqrt(dx * dx + dy * dy)
            if d < 46:
                if d > 38:
                    pixels[idx] = 240
                    pixels[idx + 1] = 180
                    pixels[idx + 2] = 20
                else:
                    pixels[idx] = 50
                    pixels[idx + 1] = 55
                    pixels[idx + 2] = 65

# Multimeter Test Lead Wires (Curved Red & Black Cables)
for step in range(350):
    t = step / 350.0
    # Red probe wire curving to the right
    rx = int(220 + t * 450 + math.sin(t * 5.0) * 40)
    ry = int(1800 - t * 180 + math.cos(t * 3.0) * 30)
    if 0 <= rx < width and 0 <= ry < height:
        for ox in range(-3, 4):
            for oy in range(-3, 4):
                idx = ((ry + oy) * width + (rx + ox)) * 3
                pixels[idx] = 220     # Bright Red wire
                pixels[idx + 1] = 35
                pixels[idx + 2] = 35

# B: Heavy-duty Screwdriver & Solar Cable Stripper / Wrench (Right side: x=580 to 980, y=1480 to 1840)
# Insulated 1000V Screwdriver (Diagonal)
for step in range(360):
    t = step / 360.0
    sx = int(580 + t * 340)
    sy = int(1780 - t * 240)
    # Handle (first 40% of length is thick orange/red insulated handle)
    is_handle = (t < 0.45)
    thickness = 14 if is_handle else 5
    for o in range(-thickness, thickness + 1):
        px = sx + int(o * 0.6)
        py = sy - int(o * 0.8)
        if 0 <= px < width and 0 <= py < height:
            idx = (py * width + px) * 3
            if is_handle:
                # 1000V VDE Red/Orange grip
                pixels[idx] = 240
                pixels[idx + 1] = 90
                pixels[idx + 2] = 20
            else:
                # Chrome vanadium steel shaft
                pixels[idx] = 195
                pixels[idx + 1] = 205
                pixels[idx + 2] = 215

# Adjustable Wrench (Right side)
for step in range(320):
    t = step / 320.0
    wx = int(680 + t * 280)
    wy = int(1850 - t * 180)
    is_head = (t > 0.75)
    thickness = 18 if is_head else 9
    for o in range(-thickness, thickness + 1):
        px = wx + int(o * 0.5)
        py = wy - int(o * 0.85)
        if 0 <= px < width and 0 <= py < height:
            idx = (py * width + px) * 3
            # Steel metallic finish
            shine = 160 + int(math.sin(o * 0.4) * 45)
            pixels[idx] = clamp(shine * 0.95)
            pixels[idx + 1] = clamp(shine)
            pixels[idx + 2] = clamp(shine * 1.05)

# 6. Apply Atmospheric Ambient Overlay
# Dark rich vignette so all text and UI inputs placed on top are crystal clear
for y in range(height):
    row_offset = y * width * 3
    # Top overlay (0.55), middle panel overlay (0.65), bottom workbench overlay (0.75)
    ny = y / height
    alpha = 0.55 + ny * 0.28
    dark_r, dark_g, dark_b = 6, 12, 22
    
    for x in range(width):
        idx = row_offset + x * 3
        # Vignette from sides
        edge_factor = abs(x / float(width) - 0.5) * 2.0 # 0 at center, 1 at edge
        total_darkness = min(0.85, alpha + edge_factor * 0.15)
        
        orig_r = pixels[idx]
        orig_g = pixels[idx + 1]
        orig_b = pixels[idx + 2]
        
        pixels[idx] = clamp(orig_r * (1.0 - total_darkness) + dark_r * total_darkness)
        pixels[idx + 1] = clamp(orig_g * (1.0 - total_darkness) + dark_g * total_darkness)
        pixels[idx + 2] = clamp(orig_b * (1.0 - total_darkness) + dark_b * total_darkness)

ppm_path = "/tmp/solar_bg.ppm"
png_path = "/app/src/main/res/drawable/img_solar_bg.png"

print(f"Writing PPM file to {ppm_path}...")
header = f"P6\n{width} {height}\n255\n".encode("ascii")
with open(ppm_path, "wb") as f:
    f.write(header)
    f.write(pixels)

print(f"Converting PPM to PNG at {png_path}...")
res = subprocess.run(["convert", ppm_path, png_path], capture_output=True, text=True)
if res.returncode == 0:
    print("SUCCESS: Generated img_solar_bg.png!")
    os.remove(ppm_path)
else:
    print(f"ERROR: {res.stderr}")
