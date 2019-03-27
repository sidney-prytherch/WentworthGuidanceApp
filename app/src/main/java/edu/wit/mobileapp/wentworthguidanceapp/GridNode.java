package edu.wit.mobileapp.wentworthguidanceapp;

public class GridNode {
    int x;
    int y;
    int id = -1;
    boolean isProject;
    GridNode left = null;
    GridNode right = null;
    GridNode up = null;
    GridNode down = null;

    public GridNode(int x, int y) {
        this.isProject = false;
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public GridNode(int x, int y, int projectId) {
        this.isProject = true;
        this.x = x;
        this.y = y;
        this.id = projectId;
    }

    // this is a boolean array. position 0 is up, 1 is right, 2 down, 3 left
    public boolean[] getValidDirections() {
        boolean[] directions = new boolean[4];
        if (this.up != null && !this.up.isProject) directions[0] = true;
        if (this.right != null && !this.right.isProject) directions[1] = true;
        if (this.down != null && !this.down.isProject) directions[2] = true;
        if (this.left != null && !this.left.isProject) directions[3] = true;
        return directions;
    }
}