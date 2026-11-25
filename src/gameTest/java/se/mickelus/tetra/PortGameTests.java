package se.mickelus.tetra;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import se.mickelus.tetra.aspect.TetraEnchantmentHelper;
import se.mickelus.tetra.effect.EffectHelper;
import se.mickelus.tetra.effect.potion.SatiatedPotionEffect;
import se.mickelus.tetra.blocks.scroll.ScrollData;
import se.mickelus.tetra.blocks.forged.extractor.CoreExtractorBaseBlockEntity;
import se.mickelus.tetra.blocks.forged.transfer.TransferUnitBlockEntity;
import se.mickelus.tetra.blocks.holo.HolosphereBlock;
import se.mickelus.tetra.blocks.holo.HolosphereBlockEntity;
import se.mickelus.tetra.blocks.rack.RackBlock;
import se.mickelus.tetra.blocks.rack.RackTile;
import se.mickelus.tetra.blocks.workbench.BasicWorkbenchBlock;
import se.mickelus.tetra.blocks.workbench.WorkbenchTile;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.items.cell.ThermalCellItem;
import se.mickelus.tetra.items.modular.ItemModularHandheld;
import se.mickelus.tetra.items.modular.impl.bow.ModularBowItem;
import se.mickelus.tetra.items.modular.impl.crossbow.ModularCrossbowItemImpl;
import se.mickelus.tetra.items.modular.impl.toolbelt.inventory.QuickslotInventory;
import se.mickelus.tetra.items.modular.impl.toolbelt.ToolbeltHelper;
import se.mickelus.tetra.module.ModuleRegistry;
import se.mickelus.tetra.module.ItemUpgradeRegistry;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.trades.TradeHandler;
import se.mickelus.tetra.util.ItemStackTagHelper;

import java.util.List;
import java.util.Optional;

@GameTestHolder(TetraMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortGameTests {
    private static final String TEMPLATE = "forged_ruins/crate_2_1";

    @GameTest(template = TEMPLATE)
    public static void modularShieldUsesItsOwnDisableCooldown(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack shield = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.SHIELD));
        var item = (ItemModularHandheld) shield.getItem();
        int cooldown = (int) (item.getCooldownBase(shield) * 20 * 0.75);
        helper.assertTrue(cooldown > 0 && cooldown < 100, "Expected a shield with a custom cooldown shorter than vanilla");
        player.setItemInHand(InteractionHand.MAIN_HAND, shield);
        player.startUsingItem(InteractionHand.MAIN_HAND);
        player.disableShield();
        helper.assertTrue(!player.isUsingItem(), "Disabling the shield did not stop blocking");
        for (int tick = 0; tick < cooldown; tick++) {
            player.getCooldowns().tick();
        }
        helper.assertTrue(!player.getCooldowns().isOnCooldown(item), "Vanilla cooldown overwrote the modular shield cooldown");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void satiatedAbsorbsActionExhaustionButNotHealing(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var satiated = EffectHelper.effectHolder(SatiatedPotionEffect.instance);
        player.addEffect(new MobEffectInstance(satiated, 200, 2));
        player.causeFoodExhaustion(2);
        helper.assertTrue(player.getFoodData().getExhaustionLevel() == 0,
                "Satiated did not absorb exhaustion applied before the next player tick");
        helper.assertTrue(player.getEffect(satiated).getAmplifier() == 0, "Expected one point of Satiated protection left");

        // Natural regeneration writes to FoodData directly, bypassing Player.causeFoodExhaustion.
        player.setHealth(player.getMaxHealth() - 4);
        for (int tick = 0; tick < 10; tick++) {
            player.tick();
        }
        helper.assertTrue(player.getHealth() > player.getMaxHealth() - 4, "Natural regeneration did not run");
        helper.assertTrue(player.getFoodData().getExhaustionLevel() > 0, "Satiated absorbed natural regeneration exhaustion");
        var remaining = player.getEffect(satiated);
        helper.assertTrue(remaining != null && remaining.getAmplifier() == 0, "Healing consumed Satiated protection");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blockEntitiesSaveAndLoad(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var blocks = BuiltInRegistries.BLOCK.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals(TetraMod.MOD_ID))
                .filter(entry -> entry.getValue() instanceof EntityBlock)
                .toList();
        helper.assertTrue(!blocks.isEmpty(), "No Tetra block entities registered");
        for (var entry : blocks) {
            var state = entry.getValue().defaultBlockState();
            var block = (EntityBlock) entry.getValue();
            var original = block.newBlockEntity(BlockPos.ZERO, state);
            helper.assertTrue(original != null, "Missing block entity for " + entry.getKey().location());
            var restored = block.newBlockEntity(BlockPos.ZERO, state);
            restored.loadWithComponents(original.saveWithFullMetadata(registries), registries);
            helper.assertTrue(restored.getType().isValid(state), "Invalid block entity type for " + entry.getKey().location());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaToolsConvertAndPersist(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        for (var input : List.of(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.BOW, Items.CROSSBOW, Items.SHIELD, Items.TRIDENT)) {
            ItemStack vanilla = new ItemStack(input);
            vanilla.setDamageValue(3);
            ItemStack upgraded = ItemUpgradeRegistry.instance.getReplacement(vanilla);
            helper.assertTrue(upgraded.getItem() instanceof IModularItem, "No modular replacement for " + input);
            helper.assertTrue(upgraded.getMaxDamage() > 3, "Missing durability for " + input);
            helper.assertTrue(upgraded.getDamageValue() == 3, "Damage lost converting " + input);
            ItemStack restored = ItemStack.parse(registries, upgraded.save(registries)).orElseThrow();
            helper.assertTrue(ItemStack.matches(upgraded, restored), "Item changed after save/load for " + input);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void enchantedToolConvertsInWorkbench(GameTestHelper helper) {
        var level = helper.getLevel();
        var sharpness = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BasicWorkbenchBlock.instance);
        var workbench = (WorkbenchTile) level.getBlockEntity(helper.absolutePos(pos));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(sharpness, 2);
        workbench.getItemHandler(null).insertItem(0, sword, false);
        ItemStack target = workbench.getTargetItemStack();
        helper.assertTrue(target.getItem() instanceof IModularItem, "Workbench failed to prepare a modular sword");
        helper.assertTrue(!SchematicRegistry.getAllSchematics().isEmpty(), "No workbench schematics loaded");
        helper.assertTrue(target.getEnchantmentLevel(sharpness) == 2, "Sharpness was lost in workbench conversion");
        ItemStack cleared = TetraEnchantmentHelper.removeAllEnchantments(target.copy());
        helper.assertTrue(cleared.getEnchantmentLevel(sharpness) == 0, "Sharpness survived enchantment removal");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void scrollComponentsSurviveSaveAndLoad(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ScrollData scroll = new ScrollData("port_test", Optional.of("details"), true, 1, 2, List.of(3, 4),
                List.of(ResourceLocation.parse("tetra:sword/basic_blade")), List.of());
        ItemStack stack = scroll.createItemStack();
        ItemStack restored = ItemStack.parse(registries, stack.save(registries)).orElseThrow();
        helper.assertTrue(scroll.equals(ScrollData.read(restored)), "Scroll content changed after save/load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void craftReplacementBlade(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BasicWorkbenchBlock.instance);
        var workbench = (WorkbenchTile) level.getBlockEntity(helper.absolutePos(pos));
        var player = helper.makeMockPlayer(GameType.CREATIVE);
        var hammer = level.getRecipeManager().byKey(ResourceLocation.parse("tetra:hammer/oak")).orElseThrow()
                .value().getResultItem(level.registryAccess()).copy();
        player.getInventory().add(hammer);

        var inventory = workbench.getItemHandler(null);
        inventory.insertItem(0, new ItemStack(Items.IRON_SWORD), false);
        inventory.insertItem(1, new ItemStack(Items.OAK_PLANKS, 2), false);
        var blade = SchematicRegistry.getSchematic(ResourceLocation.parse("tetra:sword/basic_blade"));
        helper.assertTrue(blade != null, "Basic blade schematic missing");
        workbench.setCurrentSchematic(blade, "sword/blade");
        workbench.craft(player);

        helper.assertTrue(inventory.getStackInSlot(0).getItem() instanceof IModularItem, "Blade craft did not replace the vanilla sword");
        helper.assertTrue(inventory.getStackInSlot(1).isEmpty(), "Blade craft did not consume the input materials");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolbeltAndRackRetainItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var belt = level.getRecipeManager().byKey(ResourceLocation.parse("tetra:toolbelt_modular")).orElseThrow()
                .value().getResultItem(registries).copy();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(9, belt);
        helper.assertTrue(ToolbeltHelper.findToolbelt(player) == belt, "Toolbelt lookup failed for a belt in the player inventory");
        var sword = new ItemStack(Items.IRON_SWORD);
        var slots = new QuickslotInventory(belt, registries);
        helper.assertTrue(slots.getContainerSize() > 0, "Starter toolbelt has no quickslot");
        slots.setItem(0, sword.copy());
        var restoredBelt = ItemStack.parse(registries, belt.save(registries)).orElseThrow();
        helper.assertTrue(ItemStack.matches(sword, new QuickslotInventory(restoredBelt, registries).getItem(0)),
                "Quickslot contents did not survive save/load");

        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, RackBlock.instance);
        var rack = (RackTile) level.getBlockEntity(helper.absolutePos(pos));
        rack.getItemHandler(null).insertItem(0, sword.copy(), false);
        var restoredRack = new RackTile(rack.getBlockPos(), rack.getBlockState());
        restoredRack.loadWithComponents(rack.saveWithFullMetadata(registries), registries);
        helper.assertTrue(ItemStack.matches(sword, restoredRack.getItemHandler(null).getStackInSlot(0)),
                "Rack contents did not survive save/load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void legacyToolbeltInventoryRetainsNestedItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var belt = level.getRecipeManager().byKey(ResourceLocation.parse("tetra:toolbelt_modular")).orElseThrow()
                .value().getResultItem(registries).copy();
        CompoundTag legacyStack = new CompoundTag();
        legacyStack.putString("id", "minecraft:iron_sword");
        legacyStack.putByte("Count", (byte) 1);
        legacyStack.putByte("slot", (byte) 0);
        CompoundTag legacyItemData = new CompoundTag();
        legacyItemData.putInt("Damage", 19);
        legacyStack.put("tag", legacyItemData);
        ListTag contents = new ListTag();
        contents.add(legacyStack);
        ItemStackTagHelper.mutate(belt, tag -> tag.put("quickInventory", contents));
        var restored = new QuickslotInventory(belt, registries).getItem(0);
        helper.assertTrue(restored.is(Items.IRON_SWORD) && restored.getDamageValue() == 19,
                "A nested 1.20.1 toolbelt item lost its item ID or durability");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bundledRecipesAndStructuresLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var recipes = server.getResourceManager().listResources("recipe", path ->
                path.getNamespace().equals(TetraMod.MOD_ID) && path.getPath().endsWith(".json"));
        helper.assertTrue(!recipes.isEmpty(), "No Tetra recipe resources found");
        recipes.keySet().forEach(path -> {
            var id = path.withPath(path.getPath().substring("recipe/".length(), path.getPath().length() - ".json".length()));
            helper.assertTrue(level.getRecipeManager().byKey(id).isPresent(), "Recipe did not load: " + id);
        });

        var structures = server.getResourceManager().listResources("structure", path ->
                path.getNamespace().equals(TetraMod.MOD_ID) && path.getPath().endsWith(".nbt"));
        helper.assertTrue(!structures.isEmpty(), "No Tetra structure templates found");
        structures.keySet().forEach(path -> {
            var id = path.withPath(path.getPath().substring("structure/".length(), path.getPath().length() - ".nbt".length()));
            helper.assertTrue(level.getStructureManager().get(id).isPresent(), "Structure template did not load: " + id);
        });
        var worldgen = level.registryAccess();
        helper.assertTrue(worldgen.registryOrThrow(Registries.STRUCTURE).getHolder(ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.parse("tetra:regular_ruin"))).isPresent(), "Forged ruins failed to register");
        helper.assertTrue(worldgen.registryOrThrow(Registries.PLACED_FEATURE).getHolder(ResourceKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.parse("tetra:geode_ore"))).isPresent(), "Geode generation failed to register");
        helper.assertTrue(!ModuleRegistry.instance.getAllModules().isEmpty(), "No modular item definitions loaded");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lootAndAdvancementsLoad(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var lootKeys = server.reloadableRegistries().getKeys(Registries.LOOT_TABLE);
        server.getResourceManager().listResources("loot_table", PortGameTests::isTetraJson).keySet().forEach(path ->
                helper.assertTrue(lootKeys.contains(resourceId(path, "loot_table")), "Loot table did not load: " + path));
        server.getResourceManager().listResources("advancement", PortGameTests::isTetraJson).keySet().forEach(path ->
                helper.assertTrue(server.getAdvancements().get(resourceId(path, "advancement")) != null,
                        "Advancement did not load: " + path));

        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("tetra:actions/geode"));
        var geode = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tetra:block_geode")).defaultBlockState();
        var context = new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, geode)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.IRON_PICKAXE))
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.BLOCK);
        var drops = server.reloadableRegistries().getLootTable(key).getRandomItems(context, 42L);
        helper.assertTrue(!drops.isEmpty(), "Opening a geode yielded no loot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void modularMiningCombatAndHoning(GameTestHelper helper) {
        var pick = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.IRON_PICKAXE));
        var axe = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.IRON_AXE));
        helper.assertTrue(pick.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "Iron pickaxe cannot harvest stone");
        helper.assertTrue(pick.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "Iron pickaxe cannot harvest diamond ore");
        helper.assertTrue(!pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "Iron pickaxe can harvest obsidian");
        helper.assertTrue(axe.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1, "Axe has no wood mining speed");

        var sword = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.IRON_SWORD));
        var attributes = sword.getAttributeModifiers();
        for (var attribute : List.of(Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED)) {
            helper.assertTrue(attributes.modifiers().stream().anyMatch(entry -> entry.attribute().equals(attribute)
                    && entry.slot().test(EquipmentSlot.MAINHAND)), "Sword main-hand attribute missing: " + attribute);
            helper.assertTrue(attributes.modifiers().stream().noneMatch(entry -> entry.attribute().equals(attribute)
                    && entry.slot().test(EquipmentSlot.OFFHAND)), "Sword attribute leaked into offhand: " + attribute);
        }
        var modular = (IModularItem) sword.getItem();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        modular.setHoningProgress(sword, 1);
        modular.tickProgression(player, sword, 1);
        helper.assertTrue(IModularItem.isHoneable(sword), "Usage did not make the sword eligible for honing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void extractorRetainsCharge(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tetra:core_extractor")));
        var extractor = (CoreExtractorBaseBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(extractor != null, "Core extractor did not create a block entity");
        helper.assertTrue(extractor.fill(25) == 0, "Extractor rejected charge");
        helper.assertTrue(extractor.drain(5) == 5, "Extractor did not discharge");
        var restored = new CoreExtractorBaseBlockEntity(extractor.getBlockPos(), extractor.getBlockState());
        restored.loadWithComponents(extractor.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        helper.assertTrue(restored.getCharge() == 20, "Extractor charge did not survive save/load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void thermalCellCanTransferCharge(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tetra:transfer_unit")));
        var unit = (TransferUnitBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
        var cell = new ItemStack(ThermalCellItem.instance.get());
        cell.setDamageValue(cell.getMaxDamage());
        helper.assertTrue(unit.putCell(cell), "Transfer unit rejected a thermal cell");
        helper.assertTrue(unit.fill(40) == 0 && unit.getCharge() == 40, "Thermal cell did not charge");
        helper.assertTrue(unit.drain(10) == 10 && unit.getCharge() == 30, "Thermal cell did not discharge");
        var restored = new TransferUnitBlockEntity(unit.getBlockPos(), unit.getBlockState());
        restored.loadWithComponents(unit.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        helper.assertTrue(restored.hasCell() && restored.getCharge() == 30, "Transfer unit lost its cell or charge after save/load");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void holosphereScansAndRestoresResults(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, HolosphereBlock.instance.get());
        var sphere = (HolosphereBlockEntity) level.getBlockEntity(helper.absolutePos(pos));
        var item = level.getRecipeManager().byKey(ResourceLocation.parse("tetra:holosphere")).orElseThrow()
                .value().getResultItem(level.registryAccess()).copy();
        sphere.setItemTag(ItemStackTagHelper.getOrCreateTag(item));
        sphere.toggleScanMode(true);
        helper.assertTrue(sphere.inScanMode(), "Holosphere did not enter scan mode");
        sphere.use(1, 0, 0);
        helper.assertTrue(sphere.getScanResults().size() == 4, "Expected four sampled chunks");
        var restored = new HolosphereBlockEntity(sphere.getBlockPos(), sphere.getBlockState());
        restored.loadWithComponents(sphere.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        helper.assertTrue(restored.getScanResults().size() == 4, "Holosphere did not persist scan results");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wanderingTraderOffersAndRuinsTag(GameTestHelper helper) {
        var level = helper.getLevel();
        var trader = EntityType.WANDERING_TRADER.create(level);
        int tetraOffers = 0;
        for (var listings : VillagerTrades.WANDERING_TRADER_TRADES.values()) {
            for (var listing : listings) {
                if (!listing.getClass().getPackageName().equals(TradeHandler.class.getPackageName())) {
                    continue;
                }
                var offer = listing.getOffer(trader, level.getRandom());
                helper.assertTrue(offer != null && !offer.getResult().isEmpty(), "A Tetra wandering trade has no result");
                tetraOffers++;
            }
        }
        helper.assertTrue(tetraOffers == 8, "Expected eight wandering trader offers, got " + tetraOffers);
        helper.assertTrue(level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(TradeHandler.ruinsTag)
                .filter(tag -> tag.size() == 2).isPresent(), "Cartographer's ruins tag is incomplete");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rangedWeaponsUseAmmoAndShieldBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        var ammo = new ItemStack(Items.ARROW, 8);
        player.getInventory().setItem(9, ammo);
        var bow = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.BOW));
        var bowItem = (ModularBowItem) bow.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        bowItem.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.isUsingItem(), "Modular bow did not start drawing");
        for (int tick = 0; tick <= bowItem.getDrawDuration(player, bow); tick++) {
            player.tick();
        }
        player.releaseUsingItem();
        var range = new AABB(player.blockPosition()).inflate(8);
        helper.assertTrue(level.getEntitiesOfClass(AbstractArrow.class, range).size() == 1, "Modular bow did not fire an arrow");
        helper.assertTrue(player.getInventory().countItem(Items.ARROW) == 7, "Modular bow did not use one arrow");

        var crossbow = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.CROSSBOW));
        var crossbowItem = (ModularCrossbowItemImpl) crossbow.getItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
        crossbowItem.use(level, player, InteractionHand.MAIN_HAND);
        for (int tick = 0; tick < 80; tick++) {
            player.tick();
        }
        player.releaseUsingItem();
        helper.assertTrue(crossbowItem.isLoaded(crossbow), "Modular crossbow did not load ammunition");
        crossbowItem.use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!crossbowItem.isLoaded(crossbow), "Modular crossbow did not empty after firing");
        helper.assertTrue(level.getEntitiesOfClass(AbstractArrow.class, range).size() == 2, "Modular crossbow did not fire an arrow");
        helper.assertTrue(player.getInventory().countItem(Items.ARROW) == 6, "Crossbow ammunition did not match usage");

        var shield = ItemUpgradeRegistry.instance.getReplacement(new ItemStack(Items.SHIELD));
        var shieldItem = (ItemModularHandheld) shield.getItem();
        player.setItemInHand(InteractionHand.OFF_HAND, shield);
        shieldItem.use(level, player, InteractionHand.OFF_HAND);
        helper.assertTrue(shieldItem.isShield(shield) && player.isUsingItem(), "Modular shield cannot start blocking");
        helper.succeed();
    }

    private static boolean isTetraJson(ResourceLocation path) {
        return path.getNamespace().equals(TetraMod.MOD_ID) && path.getPath().endsWith(".json");
    }

    private static ResourceLocation resourceId(ResourceLocation path, String directory) {
        return path.withPath(path.getPath().substring(directory.length() + 1, path.getPath().length() - ".json".length()));
    }
}
