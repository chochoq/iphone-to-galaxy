"""Inspect the encoded asset without modifying it. Requires Pillow."""
import sys
from PIL import Image, ImageChops, ImageStat

with Image.open(sys.argv[1]) as animation:
    assert animation.size == (320, 320), animation.size
    assert animation.n_frames == 180, animation.n_frames
    frames = []
    duration = 0
    for index in range(animation.n_frames):
        animation.seek(index)
        frame = animation.convert("RGBA")
        duration += animation.info["duration"]
        assert frame.getchannel("A").getextrema() == (0, 255)
        bounds = frame.getchannel("A").getbbox()
        assert bounds and bounds[0] > 0 and bounds[1] > 0
        assert bounds[2] < 320 and bounds[3] < 320, bounds
        frames.append(frame)
    assert duration == 6000, duration
    assert len({frame.tobytes() for frame in frames}) == 180
    differences = [sum(ImageStat.Stat(ImageChops.difference(frames[i], frames[(i + 1) % 180])).mean)
                   for i in range(180)]
    assert differences[-1] <= max(differences[:-1]) * 1.5, "Loop seam is an outlier"
    print(f"PASS: 180 unique RGBA frames, 6000ms, no clipping; seam {differences[-1]:.3f}, "
          f"max internal step {max(differences[:-1]):.3f}")
