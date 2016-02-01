package info.ganglia.jmxetric;

import info.ganglia.gmetric4j.Publisher;
import info.ganglia.gmetric4j.gmetric.GMetricSlope;
import info.ganglia.gmetric4j.gmetric.GMetricType;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/**
 * Data structure used to sample one attribute
 */
class MBeanAttribute {
	private static Logger log = Logger.getLogger(JMXetricAgent.class.getName());

	private String process;
	private String attributeName;
	private String key;
	private String canonicalName;
	private String units;
	private GMetricType type;
	private GMetricSlope slope;
	private String publishName;
	private int dmax;
	private MBeanServer mbs;
	private MBeanSampler sampler;

	public MBeanAttribute(MBeanSampler sampler, String process,
			String attributeName, String compositeKey, GMetricType type,
			String units, GMetricSlope slope, String publishName, int dmax) {
		this.sampler = sampler;
		this.process = process;
		this.key = compositeKey;
		this.canonicalName = attributeName + "." + compositeKey;
		this.attributeName = attributeName;
		this.units = units;
		this.type = type;
		this.slope = slope;
		this.publishName = publishName;
		this.dmax = dmax;
	}

	public MBeanAttribute(String process, String attributeName,
			String compositeKey, GMetricType type, String units,
			GMetricSlope slope, String publishName, int dmax) {
		this(null, process, attributeName, compositeKey, type, units, slope,
				publishName, dmax);
	}

	public MBeanAttribute(String process, String attributeName,
			GMetricType type, String units, GMetricSlope slope,
			String publishName, int dmax) {
		this(process, attributeName, null, type, units, slope, publishName,
				dmax);
	}

	public void publish(ObjectName objectName) {
		try {
			String ex = null;
			if(this.mbs == null) {
				this.mbs = ManagementFactory.getPlatformMBeanServer();
			}

			int blockedThreadCount = 0;
			if(!this.attributeName.equals("BlockedThreadCount")) {
				Object var9 = this.mbs.getAttribute(objectName, this.attributeName);
				if(var9 instanceof CompositeData) {
					CompositeData var11 = (CompositeData)var9;
					if(this.key != null) {
						Object var12 = var11.get(this.key);
						log.fine("Sampling " + objectName + " attribute " + this.canonicalName + ":" + var12);
						ex = var12.toString();
					}
				} else if(var9 != null) {
					ex = var9.toString();
					log.fine("Sampling " + objectName + " attribute " + this.canonicalName + ":" + var9);
				} else {
					log.fine("Not sampling " + objectName + " attribute " + this.canonicalName + " as value is null");
				}
			} else {
				Map gm = Thread.getAllStackTraces();
				Iterator val = gm.entrySet().iterator();

				while(val.hasNext()) {
					Map.Entry cd = (Map.Entry)val.next();
					if(Thread.State.BLOCKED == ((Thread)cd.getKey()).getState()) {
						++blockedThreadCount;
					}
				}

				ex = String.valueOf(blockedThreadCount);
			}

			if(ex != null) {
				Publisher var10 = this.sampler.getPublisher();
				var10.publish(this.process, this.publishName, ex, this.getType(), this.getSlope(), this.sampler.getDelay(), this.getDMax(), this.getUnits());
			}
		} catch (InstanceNotFoundException var7) {
			log.warning("Exception when getting " + objectName + " " + this.canonicalName);
		} catch (Exception var8) {
			log.log(Level.WARNING, "Exception when getting " + objectName + " " + this.canonicalName, var8);
		}
	}

	public String getAttributeName() {
		return attributeName;
	}

	public String getCanonicalName() {
		return canonicalName;
	}

	public String getUnits() {
		return units;
	}

	public GMetricType getType() {
		return type;
	}

	public GMetricSlope getSlope() {
		return slope;
	}

	public String getKey() {
		return key;
	}

	public int getDMax() {
		return dmax;
	}

	public MBeanSampler getSampler() {
		return sampler;
	}

	public void setSampler(MBeanSampler mBeanSampler) {
		sampler = mBeanSampler;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("attributeName=").append(attributeName);
		buf.append(" canonicalName=").append(canonicalName);
		buf.append(" units=").append(units);
		buf.append(" type=").append(type);
		buf.append(" slope=").append(slope);
		buf.append(" publishName=").append(publishName);
		buf.append(" dmax=").append(dmax);
		return buf.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (this.getClass() != obj.getClass())
			return false;
		MBeanAttribute attribute = (MBeanAttribute) obj;
		return canonicalName.equals(attribute.getCanonicalName());
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 79
				* hash
				+ (this.canonicalName != null ? this.canonicalName.hashCode()
						: 0);
		return hash;
	}
}