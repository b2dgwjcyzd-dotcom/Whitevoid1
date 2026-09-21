# WhiteVoid CREATE — Project Specification v0.1

## 1. Purpose

CREATE is the primary content-authoring system of WhiteVoid. It is a dedicated in-Minecraft 3D editor for creating editable concepts that can later be consumed by other WhiteVoid systems.

CREATE is not the main WhiteVoid GUI. The global WhiteVoid interface stays restrained and minimal; CREATE may expose deeper tools because it is a specialized editor.

Primary goals:
- create and edit 3D models inside Minecraft;
- save projects and continue them over days or weeks;
- support weapons, armor, wings, pets, cosmetics, tools, and other visual assets;
- provide precise mesh/component editing;
- provide a future animation editor with timeline/keyframes and live 3D preview;
- keep authored assets portable into Architecture and Cosmetics;
- preserve undo/redo and reliable project persistence.

## 2. Product boundaries

CREATE owns authoring. It does not own:
- the global WhiteVoid navigation shell;
- Architecture's farm/base planning workflow;
- final multiplayer/server integration;
- the final Cosmetics presentation screen.

CREATE exports authored assets to those systems through stable project/asset contracts.

## 3. Core project types

Supported project types:
- ARCHITECTURE
- MODEL
- WEAPON
- ARMOR
- WINGS
- PET
- COSMETIC

A project contains metadata plus an editable model. Future versions may add materials, UVs, animation clips, attachments, effects, and export metadata.

## 4. Editor architecture

The editor is divided by responsibility:

### Core
Owns editor lifecycle, registry, active model/project access, history, and project persistence.

### Project
Owns project metadata, serialization, save/open, dirty state, and autosave.

### Model
Owns Model, ModelNode, Transform, CubeGeometry, MeshGeometry, and model hierarchy.

### Geometry
Owns mesh operations, topology helpers, component selection, and component transforms.

### Viewport
Owns camera/projection, picking, rendering, grid/axes, gizmos, and viewport context.

### UI controllers
Translate input into focused editor operations. Controllers should remain single-responsibility and orchestration-oriented.

### History
Every completed user mutation should be represented by an atomic Command where practical. Continuous interactions are live during drag and recorded as one command on completion.

## 5. Current interaction rule

Interactive operations must not create one history entry per mouse frame.

Required pattern:
1. capture old state when interaction begins;
2. mutate the live model during the interaction;
3. compare old/new state on completion;
4. record one reversible command if something changed;
5. reset transient interaction state.

This currently applies to:
- object Move/Rotate/Scale;
- cube geometry resize;
- component Move/Rotate/Scale;
- vertex drag;
- edge drag.

## 6. History contract

Commands implement:
- execute()
- undo()
- redo() through the default command behavior
- name()

History owns undo/redo stacks and a bounded capacity.

History mutations notify the project dirty-state system. Rendering, hovering, and selection changes must not mark the project dirty.

Large modeling operations should remain atomic:
- Add Cube
- Delete Node
- Duplicate Node
- Resize
- Mirror
- Extrude
- Inset
- Bevel
- future material/UV operations
- future animation edits

When an operation changes an entire mesh, a snapshot-based mesh command is acceptable as a first stable implementation. Optimize later only if profiling demonstrates a need.

## 7. Model hierarchy rules

ModelNode:
- has a stable UUID;
- has a name;
- has one parent or is the root;
- may have children;
- has a transform;
- may have cube geometry;
- may have editable mesh geometry.

Hierarchy operations must preserve parent relationships and child ordering across undo/redo.

Root cannot be deleted or duplicated.

## 8. Mesh rules

MeshGeometry is the editable representation.

A mesh consists of:
- vertices;
- polygonal faces referencing vertex indices.

Operations must validate indices and preserve topology invariants.

Legacy cube geometry may lazily produce an editable mesh representation.

Geometry operations should be implemented below UI controllers. UI controllers decide what operation to invoke; geometry code performs the actual transformation.

## 9. Selection rules

Selection is editor state, not project content.

Supported component modes:
- vertex
- edge
- face

Selection operations include:
- single/add/remove/toggle selection;
- box selection;
- select all;
- invert;
- expand/shrink;
- boundary;
- loop/ring;
- topology path;
- select-through.

Undo/redo should restore content state. Selection restoration is optional and should only be added if it materially improves UX without coupling history to transient viewport state.

## 10. Transform rules

Object transform modes:
- Select
- Move
- Rotate
- Scale
- Geometry

Component transforms operate on selected mesh components.

Pivot modes and gizmo state are editor state and are not persisted as model content unless explicitly promoted to project settings later.

## 11. Viewport rules

Viewport rendering is an orchestrator, not a monolith.

Rendering responsibilities remain separated into:
- background;
- grid/axes;
- model;
- object gizmo;
- selection overlays;
- component gizmo;
- geometry handles;
- editor HUD.

Picking and hover detection stay separate from rendering.

## 12. Persistence

Project persistence is versioned.

Current format version: 1.

Persist:
- project metadata;
- model hierarchy;
- stable node UUIDs;
- transforms;
- cube geometry;
- mesh geometry.

Do not serialize transient interaction state.

Future schema changes must use explicit format migrations rather than silently changing the meaning of old data.

## 13. Dirty state and autosave

A project is dirty only after actual project-content mutation.

Current automatic dirty tracking is connected to CommandHistory mutations.

Autosave:
- runs from the client tick;
- operates only when an active project is dirty;
- currently uses a 30-second interval;
- saves through ProjectManager.

Future autosave work:
- non-blocking/asynchronous persistence if profiling shows file I/O impact;
- user-visible save/error state;
- recovery/backup files;
- migration-safe recovery.

## 14. CREATE UX direction

Global WhiteVoid style:
- clean;
- restrained;
- minimal;
- premium;
- no unnecessary visual noise.

CREATE itself may be information-dense, but tool organization must remain understandable.

The editor should feel closer to a purpose-built Minecraft-native 3D editor than a generic Minecraft settings screen.

Do not add decorative complexity merely to make the interface look advanced.

## 15. Planned editor phases

### Phase A — Foundation
- project persistence;
- model hierarchy;
- viewport;
- picking;
- transforms;
- mesh component selection;
- core modeling operations;
- undo/redo;
- autosave.

This phase is already substantially implemented and should be audited/refined rather than restarted.

### Phase B — Asset authoring
- materials;
- UV workflow;
- texture/pixel editing;
- asset previews;
- model attachments;
- export/import contracts.

### Phase C — Animation
Dedicated animation subsystem:
- animation clips;
- timeline;
- tracks;
- keyframes;
- interpolation/easing;
- play/pause/scrub;
- loop settings;
- real-time 3D preview;
- animation binding to model nodes;
- future animation export.

Animation must be its own subsystem, not embedded into CreateScreen.

### Phase D — Specialized asset workflows
- armor authoring;
- weapon authoring;
- wings;
- pets;
- cosmetics;
- reusable attachment points;
- preview templates.

### Phase E — Integration
- send finished CREATE assets to Architecture;
- expose finished pets/assets to Cosmetics;
- stable asset package/version format;
- compatibility validation.

## 16. Architecture integration contract

CREATE should produce a stable authored asset/project representation.

Architecture consumes only finalized/selected assets, not CREATE's internal editor state.

For farm/base concepts, Architecture may eventually use a dedicated build representation and schematic-like placement data.

CREATE must not become responsible for Architecture's entire build system.

## 17. Pet integration contract

A PET project authored in CREATE can later be published to the Cosmetics system.

The published asset should contain:
- model;
- textures/material references;
- animations;
- attachment/origin data;
- preview metadata.

Cosmetics should not depend on CREATE's live editor objects.

## 18. Performance principles

Do not optimize blindly.

Initial priorities:
- avoid unnecessary allocations during render;
- avoid rebuilding mesh data every frame;
- keep picking bounded to relevant geometry;
- keep history snapshots predictable;
- avoid blocking the render/client thread with large persistence operations.

Profile before introducing complex caching.

## 19. Reliability rules

Before accepting a subsystem as complete:
- inspect real existing code first;
- make one focused change at a time;
- preserve existing behavior;
- avoid monolithic controllers;
- perform static/manual consistency checks;
- use actual compilation/tests when available;
- never treat an unverified build as successful.

Major features should normally be developed across multiple iterations rather than expected to be correct in one generated patch.

## 20. Future command roadmap

Commands should be audited in this order:
1. hierarchy mutations;
2. transform mutations;
3. mesh mutations;
4. material/UV mutations;
5. animation mutations;
6. asset publication/import mutations.

A command should capture enough state to make undo/redo deterministic.

## 21. Explicit non-goals

Do not reintroduce the abandoned World Memory concept.

Do not turn the main WhiteVoid GUI into a flashy dashboard.

Do not put all CREATE functionality into one controller.

Do not make CREATE depend directly on Cosmetics or Architecture internals.

Do not introduce an animation editor by stuffing timeline logic into CreateScreen.

## 22. Definition of a healthy CREATE architecture

A healthy CREATE implementation has:
- small focused controllers;
- stable model/domain classes;
- rendering split by responsibility;
- geometry operations independent from UI;
- command-based mutation history;
- explicit persistence versioning;
- dirty tracking tied to actual mutations;
- autosave outside the screen;
- future animation isolated as a dedicated subsystem;
- clear integration boundaries with Architecture and Cosmetics.

This document is the architectural baseline. Future implementation changes should update the relevant section when a foundational rule changes.
