"""Print case-part origins and world bounds for repeatable hinge investigation."""
import math
import sys
import bpy
from mathutils import Matrix, Vector

model = sys.argv[sys.argv.index("--") + 1]
angle = float(sys.argv[sys.argv.index("--") + 2]) if len(sys.argv) > sys.argv.index("--") + 2 else 0.0
bpy.ops.import_scene.gltf(filepath=model)
if angle:
    lid = bpy.data.objects["uzpdkgqkIIWTYxJ"]
    hinge = Vector((0.0, 0.008, 0.0))
    lid.matrix_world = (Matrix.Translation(hinge)
                        @ Matrix.Rotation(math.radians(angle), 4, "X")
                        @ Matrix.Translation(-hinge) @ lid.matrix_world.copy())
    bpy.context.view_layer.update()
for name in ("tGOBauIOYelQQYO", "tuVYmOLGcugGNiT", "UpAdgrcTJrPyzZy", "uzpdkgqkIIWTYxJ"):
    root = bpy.data.objects[name]
    points = [obj.matrix_world @ Vector(corner)
              for obj in [root, *root.children_recursive] if obj.type == "MESH"
              for corner in obj.bound_box]
    minimum = tuple(round(min(p[axis] for p in points), 6) for axis in range(3))
    maximum = tuple(round(max(p[axis] for p in points), 6) for axis in range(3))
    print("CASE_INSPECT", name, "origin", tuple(round(v, 6) for v in root.matrix_world.translation),
          "rotation", tuple(round(v, 6) for v in root.rotation_euler),
          "min", minimum, "max", maximum)
if angle:
    for obj in bpy.data.objects["uzpdkgqkIIWTYxJ"].children_recursive:
        if obj.type != "MESH":
            continue
        points = [obj.matrix_world @ Vector(corner) for corner in obj.bound_box]
        print("LID_MESH", obj.name,
              "min", tuple(round(min(p[axis] for p in points), 6) for axis in range(3)),
              "max", tuple(round(max(p[axis] for p in points), 6) for axis in range(3)))
