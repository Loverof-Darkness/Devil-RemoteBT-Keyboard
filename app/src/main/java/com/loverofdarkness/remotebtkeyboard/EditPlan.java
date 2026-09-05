package com.loverofdarkness.remotebtkeyboard;

final class EditPlan {
    final String target; int deletes; int index;
    EditPlan(String remote,String target){
        this.target=target; int p=0, n=Math.min(remote.length(),target.length());
        while(p<n && remote.charAt(p)==target.charAt(p)) p++;
        deletes=remote.length()-p; index=p;
    }
    boolean done(){return deletes==0&&index==target.length();}
}
