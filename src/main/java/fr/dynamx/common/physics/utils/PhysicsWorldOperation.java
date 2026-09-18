package fr.dynamx.common.physics.utils;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import fr.dynamx.api.physics.BulletShapeType;
import fr.dynamx.api.physics.IPhysicsWorld;
import fr.dynamx.client.handlers.ClientDebugSystem;
import fr.dynamx.common.DynamXMain;
import fr.dynamx.common.entities.PhysicsEntity;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * An operation made on the physics world, in the physics thread <br>
 * See {@link PhysicsWorldOperationType} for a list of available operations <br>
 * You can add a callback to, for example, add a joint after adding a {@link com.jme3.bullet.objects.PhysicsRigidBody}. The callback is called immediately after this operation
 *
 * @param <A> The added/removed object type
 */
public class PhysicsWorldOperation<A> {
    /**
     * Max number of physics ticks an ADD_CONSTRAINT operation can be postponed while waiting for the bodies of the joint to be added to the physics world
     */
    private static final int MAX_DEFERRED_ATTEMPTS = 100;

    private final PhysicsWorldOperationType operation;
    private final A object;
    @Nullable
    private final Callable<PhysicsWorldOperation<?>> callback;
    private int attempts;
    private boolean deferred;

    /**
     * Creates a new PhysicsWorldOperation with no callback
     */
    public PhysicsWorldOperation(PhysicsWorldOperationType operation, A object) {
        this(operation, object, null);
    }

    /**
     * Creates a new PhysicsWorldOperation with a callback
     */
    public PhysicsWorldOperation(PhysicsWorldOperationType operation, A object, @Nullable Callable<PhysicsWorldOperation<?>> callback) {
        this.operation = operation;
        this.object = object;
        this.callback = callback;
    }

    /**
     * Modifies the PhysicsWorld, executing this operation
     *
     * @param physicsWorld  The DynamX PhysicsWorld
     * @param dynamicsWorld The bullet PhysicsSpace
     * @param joints        The cache of added joints
     * @param entities      The cache of added entities
     */
    public void execute(IPhysicsWorld physicsWorld, PhysicsSpace dynamicsWorld, Set<PhysicsJoint> joints, HashSet<PhysicsEntity<?>> entities) {
        deferred = false;
        if (object != null) {
            switch (operation) {
                case ADD_VEHICLE:
                case ADD_OBJECT:
                    dynamicsWorld.addCollisionObject((PhysicsCollisionObject) object);
                    if (physicsWorld.getWorld().isRemote && object instanceof PhysicsRigidBody && ((PhysicsRigidBody) object).getUserObject() instanceof BulletShapeType && !((BulletShapeType<?>) ((PhysicsRigidBody) object).getUserObject()).getType().isTerrain()) {
                        ClientDebugSystem.trackedRigidBodies.put(((PhysicsCollisionObject) object).nativeId(), (PhysicsRigidBody) object);
                    }
                    break;
                case REMOVE_VEHICLE:
                case REMOVE_OBJECT:
                    PhysicsCollisionObject collisionObject = (PhysicsCollisionObject) object;
                    if (isInSpace(dynamicsWorld, collisionObject)) {
                        // Bullet requirement: a constraint must never stay in the world without its two bodies. Otherwise the island
                        // manager reads an invalid island tag at the next step: native crash in btUnionFind::find (thread DynamXWorld).
                        // So the joints of this body are removed before the body itself, in the same physics tick.
                        if (collisionObject instanceof PhysicsBody) {
                            removeJointsOf((PhysicsBody) collisionObject, dynamicsWorld, joints);
                        }
                        dynamicsWorld.removeCollisionObject(collisionObject);
                    } else if (DynamXMain.log.isDebugEnabled()) {
                        DynamXMain.log.debug("Ignoring removal of " + collisionObject + " : it is not in the physics world (already removed ?)");
                    }
                    if (physicsWorld.getWorld().isRemote && object instanceof PhysicsRigidBody && ((PhysicsRigidBody) object).getUserObject() instanceof BulletShapeType && !((BulletShapeType<?>) ((PhysicsRigidBody) object).getUserObject()).getType().isTerrain()) {
                        ClientDebugSystem.trackedRigidBodies.remove(((PhysicsCollisionObject) object).nativeId());
                    }
                    break;
                case ADD_ENTITY:
                    if (!entities.add((PhysicsEntity<?>) object)) {
                        DynamXMain.log.fatal("Entity " + object + " is already registered, please report this !");
                    }
                    ((PhysicsEntity<?>) object).isRegistered = PhysicsEntity.EnumEntityPhysicsRegistryState.REGISTERED;
                    break;
                case REMOVE_ENTITY:
                    PhysicsEntity<?> et = (PhysicsEntity<?>) object;
                    entities.remove(et);
                    Runnable task = () -> {
                        List<PhysicsEntity> physicsEntities = et.world.getEntitiesWithinAABB(PhysicsEntity.class, et.getEntityBoundingBox().expand(10, 10, 10));
                        for (PhysicsEntity entity : physicsEntities) {
                            if (entity != et) {
                                entity.forcePhysicsActivation();
                            }
                        }
                    };
                    DynamXMain.proxy.scheduleTask(et.world, task);
                    break;
                case ADD_CONSTRAINT:
                    PhysicsJoint joint = (PhysicsJoint) object;
                    if (joints.contains(joint)) {
                        DynamXMain.log.fatal("PhysicsJoint " + object + " is already registered, please report this !");
                        break;
                    }
                    PhysicsBody missingBody = findBodyNotInSpace(dynamicsWorld, joint);
                    if (missingBody != null) {
                        // Adding this constraint now would make Bullet step with a body that is not in the world: native crash.
                        // The body is probably queued for a later physics tick (entity being spawned): retry later.
                        attempts++;
                        if (attempts <= MAX_DEFERRED_ATTEMPTS) {
                            deferred = true;
                            return; // the callback (if any) will be fired when the joint is really added
                        }
                        DynamXMain.log.error("Cannot add PhysicsJoint " + joint + " : body " + missingBody + " is still not in the physics world after "
                                + attempts + " physics ticks. Discarding the joint to prevent a crash of the physics engine.");
                        break;
                    }
                    joints.add(joint);
                    dynamicsWorld.addJoint(joint);
                    break;
                case REMOVE_CONSTRAINT:
                    if (joints.contains(object)) {
                        joints.remove(object);
                        dynamicsWorld.removeJoint((PhysicsJoint) object);
                    }
                    break;
            }
        }
        if (callback != null) {
            PhysicsWorldOperation<?> operation = null;
            try {
                operation = callback.call();
                if (operation != null)
                    operation.execute(physicsWorld, dynamicsWorld, joints, entities);
            } catch (Exception e) {
                DynamXMain.log.fatal("Exception while executing callback of " + this + ". Callback: " + callback, e);
            }
        }
    }

    /**
     * @return True if the last call to execute postponed this operation: it must be executed again at the next physics tick
     */
    public boolean isDeferred() {
        return deferred;
    }

    private static boolean isInSpace(PhysicsSpace dynamicsWorld, PhysicsCollisionObject collisionObject) {
        try {
            return dynamicsWorld.contains(collisionObject);
        } catch (IllegalArgumentException e) {
            // Unknown collision object type for this space: keep the legacy behavior
            return true;
        }
    }

    /**
     * @return The first body of the joint that is not in the physics world, or null if all the bodies are in the world
     */
    @Nullable
    private static PhysicsBody findBodyNotInSpace(PhysicsSpace dynamicsWorld, PhysicsJoint joint) {
        PhysicsBody bodyA = joint.getBodyA();
        if (bodyA != null && !isInSpace(dynamicsWorld, bodyA)) {
            return bodyA;
        }
        PhysicsBody bodyB = joint.getBodyB();
        if (bodyB != null && !isInSpace(dynamicsWorld, bodyB)) {
            return bodyB;
        }
        return null;
    }

    /**
     * Removes from the physics world all the joints connected to the given body
     */
    private static void removeJointsOf(PhysicsBody body, PhysicsSpace dynamicsWorld, Set<PhysicsJoint> joints) {
        for (PhysicsJoint joint : body.listJoints()) {
            if (dynamicsWorld.contains(joint)) {
                if (DynamXMain.log.isDebugEnabled()) {
                    DynamXMain.log.debug("Removing " + joint + " before removing its body " + body);
                }
                joints.remove(joint);
                dynamicsWorld.removeJoint(joint);
            }
        }
    }

    @Override
    public String toString() {
        return "PhysicsWorldOperation{" +
                "operation=" + operation +
                ", object=" + object +
                '}';
    }

    /**
     * All possible {@link PhysicsWorldOperation}s
     */
    public enum PhysicsWorldOperationType {
        ADD_OBJECT, REMOVE_OBJECT, ADD_ENTITY, REMOVE_ENTITY, ADD_VEHICLE, REMOVE_VEHICLE, ADD_CONSTRAINT, REMOVE_CONSTRAINT
    }
}
