package edu.wit.mobileapp.wentworthguidanceapp;

import java.util.ArrayList;

public class GridNode {
    int x;
    int y;
    int id = -1;
    boolean isProject;
    GridNode left = null;
    GridNode right = null;
    GridNode up = null;
    GridNode down = null;
    SquareButton button = null;

    public GridNode(int x, int y, SquareButton button) {
        this.isProject = false;
        this.x = x;
        this.y = y;
        this.button = button;
    }

    public GridNode(int x, int y, int projectId, SquareButton button) {
        this.isProject = true;
        this.x = x;
        this.y = y;
        this.id = projectId;
        this.button = button;
    }

    // gets non project directions or null in array of 4 in order of clock starting at 12
    public Direction[] getValidDirections() {
        Direction[] validGridNodeDirections = new Direction[4];
        validGridNodeDirections[0] = (this.up != null && !this.up.isProject) ? Direction.UP : null;
        validGridNodeDirections[1] = (this.right != null && !this.right.isProject) ? Direction.RIGHT : null;
        validGridNodeDirections[2] = (this.down != null && !this.down.isProject) ? Direction.DOWN : null;
        validGridNodeDirections[3] = (this.left != null && !this.left.isProject) ? Direction.LEFT : null;
        return validGridNodeDirections;
    }

    public GridNode getNeighbor(Direction direction) {
        switch (direction) {
            case UP:
                return this.up;
            case DOWN:
                return this.down;
            case LEFT:
                return this.left;
            case RIGHT:
                return this.right;
            default:
                return null;
        }
    }
}