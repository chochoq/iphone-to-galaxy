"""Build original parametric earbud/case meshes and render stable turntables in Blender."""

import argparse
import math
import os
import sys

import bpy
from mathutils import Vector


def args_after_blender():
    values = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--mode", choices=("preview", "full"), default="preview")
    return parser.parse_args(values)


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    for collection in (bpy.data.meshes, bpy.data.curves, bpy.data.materials,
                       bpy.data.cameras, bpy.data.lights):
        pass


def material(name, color, metallic=0.0, roughness=0.24, emission=None):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1.0)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    if emission is not None:
        bsdf.inputs["Emission Color"].default_value = (*emission, 1.0)
        bsdf.inputs["Emission Strength"].default_value = 4.0
    return mat


def assign(obj, mat):
    obj.data.materials.append(mat)
    for polygon in getattr(obj.data, "polygons", []):
        polygon.use_smooth = True
    return obj


def parent_to(obj, parent):
    obj.parent = parent
    return obj


def uv_sphere(name, location, scale, mat, parent=None, rotation=(0, 0, 0), segments=48):
    bpy.ops.mesh.primitive_uv_sphere_add(
        segments=segments, ring_count=24, location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    assign(obj, mat)
    if parent is not None:
        parent_to(obj, parent)
    return obj


def capsule(name, location, radius, depth, mat, parent=None, rotation=(0, 0, 0)):
    bpy.ops.mesh.primitive_cylinder_add(
        vertices=48, radius=radius, depth=depth, location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    bevel = obj.modifiers.new("Rounded ends", "BEVEL")
    bevel.width = min(radius * 0.98, depth * 0.22)
    bevel.segments = 8
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.modifier_apply(modifier=bevel.name)
    assign(obj, mat)
    if parent is not None:
        parent_to(obj, parent)
    return obj


def rounded_box(name, location, dimensions, radius, mat, parent=None, rotation=(0, 0, 0)):
    bpy.ops.mesh.primitive_cube_add(location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    obj.dimensions = dimensions
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    bevel = obj.modifiers.new("Soft corners", "BEVEL")
    bevel.width = radius
    bevel.segments = 10
    bevel.limit_method = "ANGLE"
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.modifier_apply(modifier=bevel.name)
    assign(obj, mat)
    if parent is not None:
        parent_to(obj, parent)
    return obj


def torus(name, location, major_radius, minor_radius, mat, parent=None,
          rotation=(0, 0, 0), scale=(1, 1, 1)):
    bpy.ops.mesh.primitive_torus_add(
        major_radius=major_radius, minor_radius=minor_radius,
        major_segments=48, minor_segments=16,
        location=location, rotation=rotation)
    obj = bpy.context.object
    obj.name = name
    obj.scale = scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    assign(obj, mat)
    if parent is not None:
        parent_to(obj, parent)
    return obj


def build_single_bud(name, parent, x, mirror, mats, scale=1.0, seated=False):
    white, black, tip_gray = mats["white"], mats["black"], mats["tip_gray"]
    base_z = 0.18 if not seated else 0.25
    head_z = base_z + 0.78 * scale
    stem_depth = 1.22 * scale
    stem_x = x - mirror * 0.10 * scale
    stem_z = head_z - 0.65 * scale

    capsule(f"{name}_stem", (stem_x, 0.04, stem_z), 0.155 * scale, stem_depth,
            white, parent)
    uv_sphere(f"{name}_head", (x, -0.01, head_z),
              (0.47 * scale, 0.38 * scale, 0.50 * scale), white, parent,
              rotation=(0, mirror * math.radians(13), mirror * math.radians(7)))

    tip_x = x + mirror * 0.32 * scale
    tip_y = -0.30 * scale
    tip_z = head_z - 0.01 * scale
    uv_sphere(f"{name}_tip", (tip_x, tip_y, tip_z),
              (0.31 * scale, 0.22 * scale, 0.27 * scale), white, parent,
              rotation=(math.radians(8), mirror * math.radians(12), 0))
    torus(f"{name}_tip_ring", (tip_x, tip_y - 0.205 * scale, tip_z),
          0.125 * scale, 0.035 * scale, tip_gray, parent,
          rotation=(math.radians(90), 0, 0), scale=(1.0, 1.0, 0.78))
    uv_sphere(f"{name}_tip_opening", (tip_x, tip_y - 0.219 * scale, tip_z),
              (0.105 * scale, 0.025 * scale, 0.075 * scale), tip_gray, parent)
    capsule(f"{name}_front_vent", (x - mirror * 0.06 * scale,
            -0.382 * scale, head_z + 0.13 * scale),
            0.065 * scale, 0.30 * scale, black, parent,
            rotation=(0, math.radians(90), mirror * math.radians(18)))
    uv_sphere(f"{name}_stem_sensor", (stem_x + mirror * 0.155 * scale,
            -0.115 * scale, stem_z + 0.10 * scale),
            (0.040 * scale, 0.025 * scale, 0.16 * scale), tip_gray, parent)
    capsule(f"{name}_bottom", (stem_x, 0.04, stem_z - stem_depth * 0.50),
            0.100 * scale, 0.045 * scale, black, parent)


def build_earbuds(mats):
    root = bpy.data.objects.new("Earbuds_Turntable", None)
    bpy.context.collection.objects.link(root)
    build_single_bud("Left", root, -0.78, -1, mats)
    build_single_bud("Right", root, 0.78, 1, mats)
    return root


def build_case(mats):
    white, black = mats["white"], mats["black"]
    inner, metal, green = mats["inner"], mats["metal"], mats["green"]
    root = bpy.data.objects.new("Case_Turntable", None)
    bpy.context.collection.objects.link(root)

    rounded_box("Case_Body", (0, 0, 0.03), (3.15, 1.48, 1.34), 0.35,
                white, root)
    rounded_box("Case_Inner", (0, -0.01, 0.67), (2.68, 1.10, 0.20), 0.16,
                inner, root)
    rounded_box("Open_Lid", (0, 0.59, 1.36), (3.08, 0.30, 0.88), 0.25,
                white, root, rotation=(math.radians(-8), 0, 0))
    rounded_box("Lid_Inner", (0, 0.415, 1.34), (2.65, 0.07, 0.58), 0.17,
                inner, root, rotation=(math.radians(-8), 0, 0))
    rounded_box("Rear_Hinge", (0, 0.765, 0.62), (0.90, 0.12, 0.16), 0.050,
                metal, root)
    uv_sphere("Status_LED", (0, -0.744, 0.11), (0.040, 0.024, 0.040),
              green, root, segments=32)

    build_single_bud("Case_Left", root, -0.52, -1, mats, scale=0.58, seated=True)
    build_single_bud("Case_Right", root, 0.52, 1, mats, scale=0.58, seated=True)
    return root


def look_at(obj, target):
    direction = Vector(target) - obj.location
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def setup_scene():
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 320
    scene.render.resolution_y = 320
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.film_transparent = True
    scene.render.image_settings.color_depth = "8"
    scene.render.resolution_percentage = 100
    scene.render.fps = 20
    scene.render.fps_base = 1.0
    scene.view_settings.look = "AgX - Medium Low Contrast"

    world = scene.world or bpy.data.worlds.new("World")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs["Color"].default_value = (0.035, 0.035, 0.045, 1)
    world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.75

    bpy.ops.object.camera_add(location=(0, -8.5, 2.45))
    camera = bpy.context.object
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 3.8
    look_at(camera, (0, 0, 0.55))
    scene.camera = camera

    for name, location, energy, size, color in (
        ("Key", (-4.0, -4.5, 6.0), 980, 4.0, (1.0, 0.97, 0.94)),
        ("Fill", (4.2, -2.5, 3.8), 760, 3.5, (0.90, 0.95, 1.0)),
        ("Rim", (0.0, 4.0, 5.2), 1050, 3.0, (1.0, 1.0, 1.0)),
    ):
        bpy.ops.object.light_add(type="AREA", location=location)
        light = bpy.context.object
        light.name = name
        light.data.energy = energy
        light.data.shape = "DISK"
        light.data.size = size
        light.data.color = color
        look_at(light, (0, 0, 0.5))
    return scene, camera


def set_visible(root, visible):
    root.hide_render = not visible
    for child in root.children_recursive:
        child.hide_render = not visible


def render_preview(scene, camera, earbuds, case, output):
    preview_dir = os.path.join(output, "preview")
    os.makedirs(preview_dir, exist_ok=True)
    for product, root, other, scale in (
        ("earbuds", earbuds, case, 3.25),
        ("case", case, earbuds, 3.75),
    ):
        set_visible(root, True)
        set_visible(other, False)
        camera.data.ortho_scale = scale
        for angle in (0, 90, 180, 270):
            root.rotation_euler[2] = math.radians(angle)
            scene.render.filepath = os.path.join(preview_dir, f"{product}-{angle:03d}.png")
            bpy.ops.render.render(write_still=True)


def render_full(scene, camera, earbuds, case, output):
    frame_count = 120
    for product, root, other, scale in (
        ("earbuds", earbuds, case, 3.25),
        ("case", case, earbuds, 3.75),
    ):
        frame_dir = os.path.join(output, product)
        os.makedirs(frame_dir, exist_ok=True)
        set_visible(root, True)
        set_visible(other, False)
        camera.data.ortho_scale = scale
        for frame in range(frame_count):
            root.rotation_euler[2] = math.tau * frame / frame_count
            scene.render.filepath = os.path.join(frame_dir, f"frame-{frame:03d}.png")
            bpy.ops.render.render(write_still=True)


def main():
    options = args_after_blender()
    output = os.path.abspath(options.output)
    os.makedirs(output, exist_ok=True)
    clear_scene()
    mats = {
        "white": material("Pearl white", (0.985, 0.99, 1.0), roughness=0.17),
        "black": material("Acoustic black", (0.012, 0.014, 0.018), roughness=0.38),
        "tip_gray": material("Tip mesh", (0.10, 0.11, 0.13), roughness=0.48),
        "inner": material("Case inner", (0.76, 0.78, 0.82), roughness=0.32),
        "metal": material("Hinge", (0.38, 0.40, 0.44), metallic=0.7, roughness=0.26),
        "green": material("Status green", (0.01, 0.75, 0.16), roughness=0.28,
                          emission=(0.01, 0.75, 0.16)),
    }
    earbuds = build_earbuds(mats)
    case = build_case(mats)
    scene, camera = setup_scene()
    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(output, "airpods-glance-model.blend"))
    if options.mode == "preview":
        render_preview(scene, camera, earbuds, case, output)
    else:
        render_full(scene, camera, earbuds, case, output)


if __name__ == "__main__":
    main()
