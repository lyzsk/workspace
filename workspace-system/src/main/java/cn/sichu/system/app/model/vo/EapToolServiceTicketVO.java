package cn.sichu.system.app.model.vo;

import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Data
public class EapToolServiceTicketVO {

    private String ticket;

    private String eapUrl;

    private long expiresIn;

    public static EapToolServiceTicketVO of(String ticket, String eapUrl, long expiresIn) {
        EapToolServiceTicketVO vo = new EapToolServiceTicketVO();
        vo.setTicket(ticket);
        vo.setEapUrl(eapUrl);
        vo.setExpiresIn(expiresIn);
        return vo;
    }
}
