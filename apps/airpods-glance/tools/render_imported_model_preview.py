"""Inspect and render the licensed polyman AirPods Pro glTF from fixed yaw angles.

This script intentionally renders cardinal previews before producing animation frames.
The preview is the visual gate that prevents another technically-playing but morphing
asset from reaching the Android app.
"""

import argparse
import json
import math
import os
import sys

import bpy
from mathutils import Matrix, Vector


PAIR_GROUP = "lVrInKZTaqFTEIY"
CASE_GROUP = "tGOBauIOYelQQYO"


def parse_args():
    values = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--mode", choices=("preview", "full"), default="preview")
    parser.add_argument("--size", type=int, default=512)
    parser.add_argument("--frames", type=int, default=180)
    parser.add_argument("--variants", action="store_true")
    parser.add_argument("--only", help="Comma-separated product labels to render")
    # The source scene is authored open.  Roughly 110 degrees, not 40 degrees,
    # places the lid at the closed seam.  Keeping the verified range as the
    # default prevents a front-only preview from looking closed while the side
    # view still has a floating lid.
    parser.add_argument("--closed-case-angle", type=float, default=110.0)
    parser.add_argument(
        "--closed-case-depth-scale",
        type=float,
        default=1.0,
        help="Reference-only Y-depth correction for the source lid shell",
    )
    parser.add_argument(
        "--closed-case-depth-offset",
        type=float,
        default=-0.001,
        help="Additional world-Y seam alignment after bounds centering",
    )
    parser.add_argument("--emission", type=float, default=0.08)
    parser.add_argument(
        "--look",
        choices=("medium-high", "medium-low"),
        default="medium-low",
    )
    parser.add_argument("--exposure", type=float, default=-0.10)
    parser.add_argument("--world-strength", type=float, default=0.45)
    parser.add_argument("--main-light", type=float, default=5.0)
    parser.add_argument("--lower-fill", type=float, default=5.0)
    parser.add_argument("--white-plastic-scale", type=float, default=0.30)
    parser.add_argument("--white-roughness", type=float, default=0.75)
    parser.add_argument("--white-ior-level", type=float, default=0.05)
    return parser.parse_args(values)


def clear_scene():
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)


def descendants(root):
    return {root, *root.children_recursive}


def mesh_bounds(objects):
    points = []
    for obj in objects:
        if obj.type != "MESH":
            continue
        points.extend(obj.matrix_world @ Vector(corner) for corner in obj.bound_box)
    if not points:
        raise RuntimeError("No mesh bounds found for selected product")
    minimum = Vector((min(p.x for p in points), min(p.y for p in points), min(p.z for p in points)))
    maximum = Vector((max(p.x for p in points), max(p.y for p in points), max(p.z for p in points)))
    return minimum, maximum


def set_product_visible(selected):
    selected = set(selected)
    for obj in bpy.context.scene.objects:
        if obj.type == "MESH":
            obj.hide_render = obj not in selected


def duplicate_hierarchy(root, suffix):
    originals = [root, *root.children_recursive]
    copies = {}
    for original in originals:
        duplicate = original.copy()
        duplicate.name = f"{original.name}_{suffix}"
        if original.data is not None:
            duplicate.data = original.data
        bpy.context.collection.objects.link(duplicate)
        copies[original] = duplicate
    for original in originals:
        duplicate = copies[original]
        duplicate.parent = copies.get(original.parent, original.parent)
    bpy.context.view_layer.update()
    return copies[root]


def prepare_product(roots, excluded_names=()):
    selected = set()
    for root in roots:
        selected.update(descendants(root))
    selected = {obj for obj in selected if obj.name not in excluded_names}
    minimum, maximum = mesh_bounds(selected)
    center = (minimum + maximum) * 0.5
    return {
        "roots": roots,
        "objects": selected,
        "center": center,
        "base_world": {root: root.matrix_world.copy() for root in roots},
    }


def set_product_yaw(product, radians):
    for root in product["roots"]:
        center = product.get("pivots", {}).get(root, product["center"])
        root.matrix_world = (
            Matrix.Translation(center)
            @ Matrix.Rotation(radians, 4, "Z")
            @ Matrix.Translation(-center)
            @ product["base_world"][root]
        )
    bpy.context.view_layer.update()


def tune_baked_emission(strength):
    """Retain the source's white-plastic texture without letting it wash out shading."""
    for source_material in bpy.data.materials:
        if not source_material.use_nodes:
            continue
        for node in source_material.node_tree.nodes:
            if node.type == "BSDF_PRINCIPLED":
                emission = node.inputs["Emission Color"]
                node.inputs["Emission Strength"].default_value = strength if emission.is_linked else 0.0


def tune_white_plastic(scale, roughness, ior_level):
    """Tone the source model's shared white shell without dimming black vents."""
    material = bpy.data.materials.get("UoApZRgmHmnlIkP")
    if material is None or not material.use_nodes:
        raise RuntimeError("Expected shared white-plastic material is missing")
    for node in material.node_tree.nodes:
        if node.type != "BSDF_PRINCIPLED":
            continue
        base = node.inputs["Base Color"].default_value
        for channel in range(3):
            base[channel] *= scale
        node.inputs["Roughness"].default_value = roughness
        node.inputs["Specular IOR Level"].default_value = ior_level


def look_at(obj, target):
    direction = Vector(target) - obj.location
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def setup_render(size, look, exposure, world_strength, main_light, lower_fill):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = size
    scene.render.resolution_y = size
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    scene.render.film_transparent = True
    scene.render.fps = 30
    scene.view_settings.look = {
        "medium-high": "AgX - Medium High Contrast",
        "medium-low": "AgX - Medium Low Contrast",
    }[look]
    scene.view_settings.exposure = exposure

    world = scene.world or bpy.data.worlds.new("Studio World")
    scene.world = world
    world.use_nodes = True
    background = world.node_tree.nodes.get("Background")
    background.inputs["Color"].default_value = (0.045, 0.05, 0.065, 1.0)
    background.inputs["Strength"].default_value = world_strength

    bpy.ops.object.camera_add()
    camera = bpy.context.object
    camera.name = "Turntable Camera"
    camera.data.type = "ORTHO"
    scene.camera = camera

    light_specs = (
        ("Key", (-3.2, -4.5, 5.2), main_light, 3.4, (1.0, 0.97, 0.95)),
        ("Fill", (4.0, -3.2, 2.2), main_light, 3.0, (0.94, 0.97, 1.0)),
        ("Rim", (0.8, 3.8, 4.8), main_light, 2.6, (1.0, 1.0, 1.0)),
        ("Lower fill", (0.0, -3.5, -2.0), lower_fill, 3.6, (0.96, 0.98, 1.0)),
    )
    lights = []
    for name, direction, energy, size_value, color in light_specs:
        bpy.ops.object.light_add(type="AREA")
        light = bpy.context.object
        light.name = name
        light.data.energy = energy
        light.data.shape = "DISK"
        light.data.size = size_value
        light.data.color = color
        lights.append((light, Vector(direction)))
    return scene, camera, lights


def position_camera_and_lights(camera, lights, center, span):
    distance = max(span * 3.2, 0.25)
    camera.location = center + Vector((0, -distance, span * 0.08))
    camera.data.ortho_scale = span * 1.24
    camera.data.lens = 70
    look_at(camera, center)
    for light, unit_position in lights:
        light.location = center + unit_position.normalized() * distance * 0.85
        light.data.size = max(span * 1.3, 0.08)
        look_at(light, center)


def product_span(product):
    maximum_span = 0.0
    for degrees in (0, 90, 180, 270):
        set_product_yaw(product, math.radians(degrees))
        minimum, maximum = mesh_bounds(product["objects"])
        maximum_span = max(maximum_span, maximum.x - minimum.x, maximum.z - minimum.z)
    set_product_yaw(product, 0.0)
    return maximum_span


def render_cardinals(scene, product, output_dir, label):
    target = os.path.join(output_dir, "preview", label)
    os.makedirs(target, exist_ok=True)
    for degrees in (0, 45, 90, 135, 180, 225, 270, 315):
        set_product_yaw(product, math.radians(degrees))
        scene.render.filepath = os.path.join(target, f"{label}-{degrees:03d}.png")
        bpy.ops.render.render(write_still=True)


def render_full(scene, product, output_dir, label, frame_count):
    target = os.path.join(output_dir, "frames", label)
    os.makedirs(target, exist_ok=True)
    for frame in range(frame_count):
        set_product_yaw(product, math.tau * frame / frame_count)
        scene.render.filepath = os.path.join(target, f"frame-{frame:03d}.png")
        bpy.ops.render.render(write_still=True)


def hierarchy_record(root):
    rows = []
    for obj in [root, *root.children_recursive]:
        rows.append({
            "name": obj.name,
            "type": obj.type,
            "parent": obj.parent.name if obj.parent else None,
            "dimensions": [round(value, 6) for value in obj.dimensions],
        })
    return rows


def main():
    options = parse_args()
    model_path = os.path.abspath(options.model)
    output_dir = os.path.abspath(options.output)
    os.makedirs(output_dir, exist_ok=True)

    clear_scene()
    bpy.ops.import_scene.gltf(filepath=model_path)
    tune_baked_emission(options.emission)
    tune_white_plastic(
        options.white_plastic_scale,
        options.white_roughness,
        options.white_ior_level,
    )

    pair_root = bpy.data.objects.get(PAIR_GROUP)
    case_root = bpy.data.objects.get(CASE_GROUP)
    if pair_root is None or case_root is None:
        raise RuntimeError("Expected product groups are missing from the imported glTF")

    pair = prepare_product([pair_root])

    # The source scene presents the buds just above the open case.  The connection
    # card needs the familiar seated state, so reuse the exact licensed bud meshes and
    # lower a linked hierarchy copy into the wells.  Both case and buds then rotate as
    # one product; no frame-by-frame synthesis or shape interpolation is involved.
    seated_pair_root = duplicate_hierarchy(pair_root, "Seated")
    seated_pair_root.matrix_world = (
        Matrix.Translation((0.0, 0.0, -0.028)) @ seated_pair_root.matrix_world.copy()
    )
    bpy.context.view_layer.update()
    # `ovv...` is the source scene's low-detail AirPods stand-in.  It is covered by
    # the detailed pair in the author's composed scene, but becomes an obvious dark
    # duplicate in an isolated transparent case render.  Exclude that stand-in and
    # use the detailed mesh copy above as the seated earbuds.
    case = prepare_product(
        [case_root, seated_pair_root],
        excluded_names=("ovvBCJtLtXvnMKD",),
    )
    products = [("pair", pair), ("case", case)]
    if options.variants:
        bud_roots = [bpy.data.objects.get(name) for name in
                     ("DprZyuuKYVGeqRc", "RTZiZFLcZlxaClC")]
        if any(root is None or root.parent != pair_root for root in bud_roots):
            raise RuntimeError("Expected two complete earbud groups")
        buds = [prepare_product([root]) for root in bud_roots]
        independent = prepare_product(bud_roots)
        independent["pivots"] = {
            root: bud["center"].copy() for root, bud in zip(bud_roots, buds)
        }
        if buds[0]["objects"] & buds[1]["objects"]:
            raise RuntimeError("Earbud groups overlap")
        for degrees in (0, 45, 90, 180, 270, 360):
            set_product_yaw(independent, math.radians(degrees))
            for root in bud_roots:
                local_center = independent["base_world"][root].inverted() @ independent["pivots"][root]
                if (root.matrix_world @ local_center - independent["pivots"][root]).length > 1e-6:
                    raise RuntimeError("Independent earbud pivot drifts")
        set_product_yaw(independent, 0.0)
        seated_roots = [bpy.data.objects.get(root.name + "_Seated") for root in bud_roots]
        empty_case = prepare_product([case_root], excluded_names=("ovvBCJtLtXvnMKD",))
        partials = [prepare_product([case_root, root], excluded_names=("ovvBCJtLtXvnMKD",))
                    for root in seated_roots]
        case_span = product_span(case)
        for product in [empty_case, *partials, case]:
            product["center"] = case["center"].copy()
            product["fixed_span"] = case_span
        if len(partials[0]["objects"]) >= len(case["objects"]):
            raise RuntimeError("Partial case unexpectedly contains full pair")
        closed_case_root = duplicate_hierarchy(case_root, "Closed")
        closed_lid = bpy.data.objects.get("uzpdkgqkIIWTYxJ_Closed")
        if closed_lid is None:
            raise RuntimeError("Expected duplicated lid group")
        hinge = Vector((0.0, 0.008, 0.0))
        closed_lid.matrix_world = (
            Matrix.Translation(hinge)
            @ Matrix.Rotation(math.radians(options.closed_case_angle), 4, "X")
            @ Matrix.Translation(-hinge)
            @ closed_lid.matrix_world.copy()
        )
        bpy.context.view_layer.update()
        if options.closed_case_depth_scale != 1.0:
            lid_minimum, lid_maximum = mesh_bounds(descendants(closed_lid))
            lid_center = (lid_minimum + lid_maximum) * 0.5
            depth_scale = Matrix.Diagonal(
                Vector((1.0, options.closed_case_depth_scale, 1.0, 1.0))
            )
            closed_lid.matrix_world = (
                Matrix.Translation(lid_center)
                @ depth_scale
                @ Matrix.Translation(-lid_center)
                @ closed_lid.matrix_world
            )
            bpy.context.view_layer.update()
        # The source is authored open and its empty has no hinge-local origin.  A rigid lid is
        # still valid after closing, but whole-shell bounds cannot align the visible seam: the
        # curved cap and hidden hinge geometry bias that center.  Keep the observed closed Z,
        # center the broad depth bounds, then apply the measured 1 mm seam-center correction.
        # At 320 px / 90 degrees this changes the mating rows from a four-pixel lateral step to
        # identical 92-pixel spans; the eight-angle visual gate remains the final authority.
        closed_body = bpy.data.objects.get("UpAdgrcTJrPyzZy_Closed")
        if closed_body is None:
            raise RuntimeError("Expected duplicated lower case shell")
        lid_minimum, lid_maximum = mesh_bounds(descendants(closed_lid))
        body_minimum, body_maximum = mesh_bounds(descendants(closed_body))
        alignment = Vector((
            0.0,
            (body_minimum.y + body_maximum.y - lid_minimum.y - lid_maximum.y) * 0.5,
            0.0,
        ))
        alignment.y += options.closed_case_depth_offset
        closed_lid.matrix_world = Matrix.Translation(alignment) @ closed_lid.matrix_world
        bpy.context.view_layer.update()
        closed_case = prepare_product(
            [closed_case_root], excluded_names=("ovvBCJtLtXvnMKD_Closed",)
        )
        products = [("pair", pair), ("pair-independent", independent),
                    ("bud-a", buds[0]), ("bud-b", buds[1]),
                    ("case-empty", empty_case), ("case-a", partials[0]),
                    ("case-b", partials[1]), ("case-both", case),
                    ("case-closed", closed_case)]
    scene, camera, lights = setup_render(
        options.size,
        options.look,
        options.exposure,
        options.world_strength,
        options.main_light,
        options.lower_fill,
    )

    report = {
        "source": model_path,
        "pair": hierarchy_record(pair_root),
        "case": hierarchy_record(case_root),
    }
    with open(os.path.join(output_dir, "import-hierarchy.json"), "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)

    if options.only:
        requested = set(options.only.split(","))
        if requested - {label for label, _ in products}:
            raise RuntimeError("Unknown requested product label")
        products = [(label, product) for label, product in products if label in requested]
    for label, product in products:
        set_product_visible(product["objects"])
        span = product.get("fixed_span") or product_span(product)
        set_product_yaw(product, 0.0)
        position_camera_and_lights(camera, lights, product["center"], span)
        if options.mode == "preview":
            render_cardinals(scene, product, output_dir, label)
        else:
            render_full(scene, product, output_dir, label, options.frames)
        set_product_yaw(product, 0.0)

    bpy.ops.wm.save_as_mainfile(filepath=os.path.join(output_dir, "polyman-turntable-source.blend"))


if __name__ == "__main__":
    main()
