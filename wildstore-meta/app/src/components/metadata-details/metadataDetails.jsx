const MetadataDetails = ({ record, header }) => {
    const variables = Array.isArray(record?.variables) ? record.variables : [];
    const globalAttributes = Array.isArray(record?.globalAttributes) ? record.globalAttributes : [];

    return (
        <div className="text-xs font-mono">
            <h3 className="m-0 mb-2 text-[#0056b3] text-sm break-all">
                📄 {record?.fileName?.[0] || header || 'Dataset'}
            </h3>

            <div className="border-b border-[#eee] pb-1 mb-1">
                <strong>Digest:</strong> {record?.digestString ?? 'N/A'}
                <br />
                <strong>Domain:</strong> {record?.domain ?? 'N/A'}
                <br />
                <strong>Size:</strong> {record?.fileSize ?? 'N/A'}
                <br />
                <strong>Path:</strong> {record?.filePath?.[0] || '(unknown)'}
            </div>

            <div className="bg-[#f9f9f9] p-1 rounded">
                <strong>Variables:</strong>
                <ul className="my-1 pl-5 text-[#444] list-disc">
                    {variables.slice(0, 12).map((variable, index) => {
                        const attrs = Array.isArray(variable.attributeList) ? variable.attributeList : [];
                        const dims = Array.isArray(variable.varDimensionList)
                            ? variable.varDimensionList.map((dimension) => `${dimension.name}=${dimension.value}`).join(', ')
                            : '';

                        const unitsAttr = attrs.find((attribute) => attribute.attributeName === 'units');
                        const units = Array.isArray(unitsAttr?.value) ? unitsAttr.value[0] : unitsAttr?.value;

                        const minValue = typeof variable.minValue === 'number' ? variable.minValue : parseFloat(variable.minValue);
                        const maxValue = typeof variable.maxValue === 'number' ? variable.maxValue : parseFloat(variable.maxValue);
                        const hasRange = Number.isFinite(minValue) && Number.isFinite(maxValue);

                        return (
                            <li key={variable.variableName || index}>
                                <strong>{variable.variableName || 'Unnamed variable'}</strong>
                                {dims && <span className="text-[#666]"> &nbsp;({dims})</span>}
                                <br />
                                <span className="text-[#666] text-[11px]">
                                    {variable.type && `type: ${variable.type}`}
                                    {units && ` • units: ${units}`}
                                    {hasRange && ` • range: ${minValue.toFixed(2)} – ${maxValue.toFixed(2)}`}
                                </span>
                            </li>
                        );
                    })}

                    {variables.length > 12 && (
                        <li>... (+{variables.length - 12} more)</li>
                    )}
                </ul>
            </div>

            {globalAttributes.length > 0 && (
                <div className="bg-[#f9f9f9] p-1 rounded mt-2">
                    <strong>Global Attributes:</strong>
                    <ul className="my-1 pl-5 text-[#444] list-disc">
                        {globalAttributes.map((attribute, index) => (
                            <li key={attribute.attributeName || index}>
                                <strong>{attribute.attributeName || 'Attribute'}</strong>
                                {attribute.value != null && (
                                    <span className="text-[#666]">: {Array.isArray(attribute.value) ? attribute.value.join(', ') : attribute.value}</span>
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default MetadataDetails;