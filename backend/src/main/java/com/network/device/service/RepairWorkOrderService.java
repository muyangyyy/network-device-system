package com.network.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.network.device.dto.*;
import com.network.device.entity.RepairHardwareDetail;
import com.network.device.entity.RepairWorkOrder;
import com.network.device.vo.AttachmentVO;
import com.network.device.vo.OrderLogVO;
import com.network.device.vo.RepairWorkOrderVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RepairWorkOrderService {

    Page<RepairWorkOrderVO> listOrders(RepairOrderQueryDTO queryDTO);

    RepairWorkOrder getOrderById(Long id);

    RepairWorkOrderVO getOrderVOById(Long id);

    RepairWorkOrder createOrder(RepairWorkOrderDTO dto);

    /** 更新工单基本信息（仅覆盖前端提交的非空字段） */
    RepairWorkOrderVO updateOrder(Long id, RepairWorkOrderDTO dto);

    void submitOrder(Long id);

    void assignOrder(Long id, AssignDTO dto);

    void startProcessing(Long id);

    void completeOrder(Long id, CompleteDTO dto);

    /**
     * 验收工单。
     *
     * @param dto 验收请求体，可为 null；备注为空时日志记为「验收通过」
     */
    void acceptOrder(Long id, AcceptDTO dto);

    void rejectOrder(Long id, RejectDTO dto);

    void delayOrder(Long id, DelayDTO dto);

    void repairAgain(Long id);

    /** 关闭工单（仅已验收状态可关闭）。关闭后可发起返修。 */
    void closeOrder(Long id);

    void cancelOrder(Long id);

    void saveHardwareDetail(Long workOrderId, CompleteDTO.HardwareDetailDTO detail);

    void saveDebugDetail(Long workOrderId, CompleteDTO.DebugDetailDTO detail);

    void saveOpticalDetail(Long workOrderId, CompleteDTO.OpticalDetailDTO detail);

    List<OrderLogVO> getOrderLogs(Long workOrderId);

    List<RepairWorkOrderVO> exportOrders(RepairOrderQueryDTO queryDTO);

    Page<RepairWorkOrderVO> listOrdersByDeviceId(Long deviceId, RepairOrderQueryDTO queryDTO);

    /** 查询工单附件列表 */
    List<AttachmentVO> listAttachments(Long workOrderId);

    /** 上传工单附件 */
    AttachmentVO uploadAttachment(Long workOrderId, MultipartFile file);

    /** 删除工单附件 */
    void deleteAttachment(Long workOrderId, Long attachmentId);

    /** 删除工单（级联清理附件、状态日志、维修明细） */
    void deleteOrder(Long id);
}
