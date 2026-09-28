import { attributeOptions } from "./attributeOptions.js";


export default function DynamicAttributeField({ field, value = "", onChange, required = field.requiredField }) {
    const options = attributeOptions(field);
    const props = { value, required, onChange: event => onChange(event.target.value) };
    let input;
    if (field.inputType === "TEXTAREA") input = <textarea {...props} rows="3" />;
    else if (field.inputType === "MULTI_SELECT") input = <select multiple required={required} value={value ? value.split(",") : []} onChange={event => onChange(Array.from(event.target.selectedOptions, option => option.value).join(","))}>{options.map(option => <option key={option}>{option}</option>)}</select>;
    else if (field.inputType === "SELECT" || field.inputType === "BOOLEAN") input = <select {...props}><option value="">Select</option>{(field.inputType === "BOOLEAN" ? ["Yes", "No"] : options).map(option => <option key={option}>{option}</option>)}</select>;
    else input = <input {...props} type={field.inputType === "NUMBER" ? "number" : "text"} step={field.inputType === "NUMBER" ? "any" : undefined} maxLength={4000} />;
    return <label>{field.displayLabel}{required && " *"}{input}</label>;
}
