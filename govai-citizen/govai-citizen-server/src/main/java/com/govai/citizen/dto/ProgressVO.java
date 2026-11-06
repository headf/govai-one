package com.govai.citizen.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProgressVO {

    private Long instanceId;
    private String status;
    private String currentNode;
    private String submitTime;
    private List<Node> nodes;

    @Data
    public static class Node {
        private String name;
        private String status;
        private String handleTime;
        private String assignee;
    }
}
