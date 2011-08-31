package bme.iclef.predict;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to represent a predicted label.
 * 
 * @author illes
 */
public class Prediction {
	
	private static Map<String, Label> labelRegistry = new HashMap<String, Label>();
	private static Map<String, Group> groupRegistry = new HashMap<String, Group>();
	private static Map<Group, Set<Label>> labelGroupRegistry = new HashMap<Group, Set<Label>>();
	
	private final int id;
	
	private String comment;

	public static enum Group {
		RADIOLOGY, MICROSCOPY, PHOTOGRAPH, GRAPHIC, OTHER;
		private Group() {
			groupRegistry.put(this.name(), this);
			labelGroupRegistry.put(this, new HashSet<Label>());
		}
		public static Set<String> codeSet() {
			return groupRegistry.keySet();
		}
		
		public Set<Label> labels()
		{
			return labelGroupRegistry.get(this);
		}
	};

	public static enum Label {
		/** 3D: 3d reconstruction */
		_3D_ThreeDee("3D", Group.OTHER),
		/** AN: angiography */
		AN_Angiography("AN", Group.RADIOLOGY),
		/** CM: compound figure (more than one type of image) */
		CM_CompoundFigure("CM", Group.OTHER),
		/** CT: computed tomography */
		CT_ComputedTomography("CT", Group.RADIOLOGY),
		/** DM: dermatology */
		DM_Dermatology("DM", Group.PHOTOGRAPH),
		/** DR: drawing */
		DR_Drawing("DR", Group.GRAPHIC),
		/** EM: electronMicroscopy */
		EM_ElectronMicroscopy("EM", Group.MICROSCOPY),
		/** EN: endoscopic imaging */
		EN_Endoscope("EN", Group.PHOTOGRAPH),
		/** FL: fluoresence */
		FL_Fluorescense("FL", Group.MICROSCOPY),
		/** GL: gel */
		GL_Gel("GL", Group.MICROSCOPY),
		/** GX: graphs */
		GX_Graphs("GX", Group.GRAPHIC),
		/** GR: gross pathology */
		GR_GrossPathology("GR", Group.PHOTOGRAPH),
		/** HX: histopathology */
		HX_Histopathology("HX", Group.MICROSCOPY),
		/** MR: magnetic resonance imaging */
		MR_MagneticResonance("MR", Group.RADIOLOGY),
		/** PX: general photo */
		PX_Photo("PX", Group.PHOTOGRAPH),
		/** RN: retinograph */
		RN_Retinograph("RN", Group.PHOTOGRAPH),
		/** US: ultrasound */
		US_Ultrasound("US", Group.RADIOLOGY),
		/** XR: x-ray */
		XR_XRay("XR", Group.RADIOLOGY),
		
		
		UnspecifiedRadiology(null, Group.RADIOLOGY),
		UnspecifiedGraphic(null, Group.GRAPHIC),
		UnspecifiedMicroscopy(null, Group.MICROSCOPY),
		UnspecifiedPhotograph(null, Group.PHOTOGRAPH),
		;
		
		public final Group group;
		public final String code;

		public static Label getDefault() {
			return PX_Photo;
		}

		public static Label forCode(String code)
		{
			return labelRegistry.get(code);
		}
		
		public static Set<String> codeSet(){
			return labelRegistry.keySet();
		}

		private Label(String code, Group group) {
			this.code = code;
			this.group = group;
			
			if (code != null)
				labelRegistry.put(code, this);
			
			labelGroupRegistry.get(group).add(this);
		}
	}

	public Prediction(final Label label, final float confidence, final int id ) {
		this.label = label;
		this.confidence = confidence;
		this.id = id;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public int getId() {
		return id;
	}

	final public Label label;
	public float confidence;

	/**
	 * Uses label only the label as a key.
	 */
	@Override
	public int hashCode() {
		return label.hashCode();
	}
}
