package com.rehab.module.order.controller;

import com.rehab.common.PageResult;
import com.rehab.common.Result;
import com.rehab.module.order.entity.MedicalOrder;
import com.rehab.module.order.service.OrderService;
import com.rehab.module.task.entity.Task;
import com.rehab.module.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final TaskService taskService;

    /**
     * List orders with optional filters.
     */
    @GetMapping
    public Result<PageResult<MedicalOrder>> list(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listOrders(patientId, status, page, size));
    }

    /**
     * Get order detail.
     */
    @GetMapping("/{id}")
    public Result<MedicalOrder> getById(@PathVariable Long id) {
        return Result.ok(orderService.getById(id));
    }

    /**
     * Create a new medical order.
     */
    @PostMapping
    public Result<MedicalOrder> create(@RequestBody MedicalOrder order) {
        return Result.ok(orderService.createOrder(order));
    }

    /**
     * Update an existing order (only DRAFT status).
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody MedicalOrder order) {
        order.setId(id);
        orderService.updateOrder(order);
        return Result.ok();
    }

    /**
     * Approve an order with an optional review comment.
     */
    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String comment = body.getOrDefault("comment", "");
        orderService.approveOrder(id, comment);
        return Result.ok();
    }

    /**
     * Reject an order with a reason.
     */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        orderService.rejectOrder(id, reason);
        return Result.ok();
    }

    /**
     * Cancel an order.
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelOrder(id);
        return Result.ok();
    }

    @PostMapping("/{id}/generate-tasks")
    public Result<List<Task>> generateTasks(@PathVariable Long id) {
        return Result.ok(taskService.generateTasksFromOrder(id));
    }
}
