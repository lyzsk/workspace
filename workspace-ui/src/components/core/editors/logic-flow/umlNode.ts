import { HtmlNode, HtmlNodeModel } from "@logicflow/core";

class UmlModel extends HtmlNodeModel {
    setAttributes() {
        this.width = 220;
        this.height = 150;
        this.textHeight = 34;
    }
}

class UmlNode extends HtmlNode {
    setHtml(rootEl: SVGForeignObjectElement) {
        const properties = this.props.model.properties as Record<string, string>;
        const wrapper = document.createElement("div");
        wrapper.className = "uml-class-node";
        const head = document.createElement("div");
        head.className = "uml-class-head";
        head.textContent = properties.name || this.props.model.text.value || "Class";
        const attributes = document.createElement("div");
        attributes.className = "uml-class-section";
        attributes.textContent = properties.attributes || "+ id: Long";
        const methods = document.createElement("div");
        methods.className = "uml-class-section";
        methods.textContent = properties.methods || "+ save(): void";
        wrapper.append(head, attributes, methods);
        rootEl.innerHTML = "";
        rootEl.appendChild(wrapper);
    }
}

export default {
    type: "uml",
    view: UmlNode,
    model: UmlModel,
};
