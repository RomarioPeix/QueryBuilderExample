package br.com.vivo.aem.b2b.ecommerceequipments.servlet;

public class HelpSearchModel {
    private String topic;
    private String faq;

    HelpSearchModel(String topic, String faq) {
        this.topic = topic;
        this.faq = faq;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getFaq() {
        return this.faq;
    }

}
