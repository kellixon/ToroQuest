package net.torocraft.games.checkerboard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.swing.internal.plaf.basic.resources.basic;

import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockQuartz;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.torocraft.games.chess.pieces.enities.IChessPiece.Side;

public class CheckerBoard {

	private World world;
	private BlockPos blockPosition;

	private static final IBlockState STAIRS_NORTH = stairsBlock(EnumFacing.NORTH);
	private static final IBlockState STAIRS_SOUTH = stairsBlock(EnumFacing.SOUTH);
	private static final IBlockState STAIRS_WEST = stairsBlock(EnumFacing.WEST);
	private static final IBlockState STAIRS_EAST = stairsBlock(EnumFacing.EAST);
	private static final IBlockState BORDER = border();

	/*
	 * Cursor Variables (relative to a1)
	 */
	private int x;
	private int y;
	private int z;
	private IBlockState block;

	/*
	 * Draw Flags
	 */
	private boolean blockWasDrawable;
	private boolean onlyPlaceIfAir = false;

	public void generate(World world, BlockPos position) {
		this.world = world;
		blockPosition = position;
		placeCheckerBlocks();
		placeBorderBlocks();
		placeBorderStairs();
		placePodiums();
	}

	public static String getPositionName(BlockPos gameBlockPos, BlockPos coords) {
		int xLocal = coords.getX() - gameBlockPos.getX();
		int zLocal = coords.getZ() - gameBlockPos.getZ();
		String name = encodeColumnName(xLocal) + minMax(zLocal + 1, 1, 8);
		return name;
	}

	private static int minMax(int i, int min, int max) {
		if (i > max) {
			return max;
		}

		if (i < min) {
			return min;
		}

		return i;
	}

	/**
	 * Get the minecraft coordinates for a given chess position (such as a1, e5)
	 */
	public static BlockPos getPosition(BlockPos gameBlockPosition, String name) {
		int[] parsed = parseIntPosition(name);
		return gameBlockPosition.add(parsed[0], 1, parsed[1]);
	}

	private static int[] parseIntPosition(String name) {
		int[] p = { -10, -10 };

		if (name == null || name.length() != 2) {
			return p;
		}

		name = name.toLowerCase();

		if (!name.matches("[a-h][1-8]")) {
			return p;
		}

		p[0] = parseColumnName(name.substring(0, 1));
		p[1] = i(name.substring(1, 2)) - 1;
		return p;
	}

	private static int i(String substring) {
		try {
			return Integer.valueOf(substring, 10);
		} catch (Exception e) {
			return -10;
		}
	}

	private static String encodeColumnName(int i) {
		switch (i) {
		case 0:
			return "a";
		case 1:
			return "b";
		case 2:
			return "c";
		case 3:
			return "d";
		case 4:
			return "e";
		case 5:
			return "f";
		case 6:
			return "g";
		case 7:
			return "h";
		}
		return "a";
	}

	private static int parseColumnName(String s) {
		if (s == null || s.length() != 1) {
			return -10;
		}

		if (s.equals("a")) {
			return 0;
		} else if (s.equals("b")) {
			return 1;
		} else if (s.equals("c")) {
			return 2;
		} else if (s.equals("d")) {
			return 3;
		} else if (s.equals("e")) {
			return 4;
		} else if (s.equals("f")) {
			return 5;
		} else if (s.equals("g")) {
			return 6;
		} else if (s.equals("h")) {
			return 7;
		}

		return -10;
	}

	private static final BlockPos BLACK_PODIUM = new BlockPos(3, -1, -2);
	private static final BlockPos WHITE_PODIUM = new BlockPos(3, -1, 9);

	private void placePodiums() {
		setCursor(WHITE_PODIUM);
		placePodium(EnumFacing.NORTH);

		setCursor(BLACK_PODIUM);
		placePodium(EnumFacing.SOUTH);
	}

	private void setCursor(BlockPos pos) {
		x = pos.getX();
		y = pos.getY();
		z = pos.getZ();
	}

	private void placePodium(EnumFacing facing) {
		block = BORDER;
		drawLine(Axis.X, 2);

		block = Blocks.chest.getDefaultState().withProperty(BlockChest.FACING, facing);
		y++;
		drawLine(Axis.X, -2);

		/*
		 * if (BlockChest.FACING.equals(facing.SOUTH)) { blackChest =
		 * ((BlockChest)
		 * world.getBlockState(cursorCoords()).getBlock()).getLockableContainer(
		 * world, cursorCoords()); } else { whiteChest = ((BlockChest)
		 * world.getBlockState(cursorCoords()).getBlock()).getLockableContainer(
		 * world, cursorCoords()); }
		 */
	}

	public static ILockableContainer getWhiteChest(World world, BlockPos gameBlockPos) {
		return getChestAtCursor(world, gameBlockPos, WHITE_PODIUM);
	}

	public static ILockableContainer getBlackChest(World world, BlockPos gameBlockPos) {
		return getChestAtCursor(world, gameBlockPos, BLACK_PODIUM);
	}

	private static ILockableContainer getChestAtCursor(World world, BlockPos gameBlockPos, BlockPos offset) {

		BlockPos chestLocation = gameBlockPos.add(offset).add(0, 2, 0);

		try {
			return ((BlockChest) world.getBlockState(chestLocation).getBlock()).getLockableContainer(world,
					chestLocation);
		} catch (ClassCastException e) {
			return null;
		}
	}

	private void placeBorderBlocks() {
		block = BORDER;
		y = 0;
		z = x = -1;
		drawLine(Axis.X, 10);
		drawLine(Axis.Z, 10);
		drawLine(Axis.X, -10);
		drawLine(Axis.Z, -10);
	}

	private void placeBorderStairs() {
		z = x = -2;
		y = 0;
		int length = 12;

		drawStairBorder(length);

		onlyPlaceIfAir = true;

		for (int i = 0; i < 50; i++) {
			z--;
			x--;
			y--;
			length += 2;
			if (!drawStairBorder(length)) {
				break;
			}
		}

		onlyPlaceIfAir = false;
	}

	private boolean drawStairBorder(int length) {
		boolean somethingDrawn = false;

		block = STAIRS_SOUTH;
		somethingDrawn = drawLine(Axis.X, length) || somethingDrawn;

		block = STAIRS_WEST;
		somethingDrawn = drawLine(Axis.Z, length) || somethingDrawn;

		block = STAIRS_NORTH;
		somethingDrawn = drawLine(Axis.X, -length) || somethingDrawn;

		block = STAIRS_EAST;
		somethingDrawn = drawLine(Axis.Z, -length) || somethingDrawn;

		return somethingDrawn;
	}

	private boolean drawLine(Axis axis, int length) {
		int l = computeTravelDistance(length);
		boolean isPositive = length >= 0;
		boolean somethingDrawn = false;
		for (int i = 0; i < l; i++) {
			placeBlock();
			if (i < l - 1) {
				if (isPositive) {
					incrementAxis(axis, 1);
				} else {
					incrementAxis(axis, -1);
				}
			}

			somethingDrawn = somethingDrawn || blockWasDrawable;
		}

		return somethingDrawn;
	}

	private int computeTravelDistance(int length) {
		return Math.abs(length);
	}

	private void incrementAxis(Axis axis, int amount) {
		switch (axis) {
		case X:
			x += amount;
			break;
		case Y:
			y += amount;
			break;
		case Z:
			z += amount;
			break;
		default:
			break;
		}
	}

	private void placeCheckerBlocks() {
		y = 0;
		for (x = 0; x < 8; x++) {
			for (z = 0; z < 8; z++) {
				block = defineCheckerBlock();
				placeBlock();
			}
		}
	}

	private void placeBlock() {
		if (okToPlaceBlock()) {
			blockWasDrawable = true;
			world.setBlockState(cursorCoords(), block);
		} else {
			blockWasDrawable = false;
		}
	}

	private boolean okToPlaceBlock() {
		return !onlyPlaceIfAir || onAirBlock();
	}

	private boolean onAirBlock() {
		IBlockState currentBlock = world.getBlockState(cursorCoords());
		return !currentBlock.isOpaqueCube();
	}

	/**
	 * Get the Minecraft coordinates of the cursor
	 */
	private BlockPos cursorCoords() {
		return blockPosition.add(x, y + 1, z);
	}

	private IBlockState defineCheckerBlock() {
		if (isWhiteBlock()) {
			return Blocks.quartz_block.getDefaultState();
		} else {
			return Blocks.obsidian.getDefaultState();
		}
	}

	private boolean isWhiteBlock() {
		return (x + z) % 2 == 0;
	}

	private static IBlockState border() {
		return Blocks.quartz_block.getDefaultState().withProperty(BlockQuartz.VARIANT, BlockQuartz.EnumType.CHISELED);
	}

	private static IBlockState stairsBlock(EnumFacing facing) {
		return Blocks.quartz_stairs.getDefaultState().withProperty(BlockStairs.FACING, facing);
	}
}
